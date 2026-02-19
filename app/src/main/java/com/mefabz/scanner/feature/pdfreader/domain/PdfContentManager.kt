package com.mefabz.scanner.feature.pdfreader.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

class PdfContentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var cachedPdfFile: File? = null
    private val mutex = Mutex()

    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    @Throws(IOException::class)
    suspend fun openPdf(uri: String) = mutex.withLock {
        closeInternal() // Close any existing
        try {
            val contentUri = Uri.parse(uri)

            val file = if (contentUri.scheme == "file") {
                val localPath = requireNotNull(contentUri.path) { "Invalid file URI path" }
                File(localPath).also { localFile ->
                    if (!localFile.exists()) {
                        throw IOException("Selected PDF file no longer exists")
                    }
                }
            } else {
                copyToCache(contentUri).also { copiedFile ->
                    cachedPdfFile = copiedFile
                }
            }

            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            if (fileDescriptor != null) {
                pdfRenderer = PdfRenderer(fileDescriptor!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw IOException("Failed to open PDF: ${e.message}", e)
        }
    }

    private fun copyToCache(uri: Uri): java.io.File {
        val inputStream = context.contentResolver.openInputStream(uri) 
            ?: throw IOException("Unable to open input stream for URI: $uri")
        
        val fileName = "temp_pdf_${System.currentTimeMillis()}.pdf"
        val file = java.io.File(context.cacheDir, fileName)
        
        inputStream.use { input ->
            java.io.FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    suspend fun renderPage(pageIndex: Int): Bitmap? = mutex.withLock {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pageCount) return null

        return withContext(Dispatchers.IO) {
            try {
                // Try high resolution first (3x) for better OCR
                renderPageInternal(pageIndex, 3f)
            } catch (e: OutOfMemoryError) {
                // Fallback to medium resolution (2x)
                try {
                    System.gc() // Suggest garbage collection
                    renderPageInternal(pageIndex, 2f)
                } catch (e2: OutOfMemoryError) {
                    // Fallback to standard resolution (1x)
                    try {
                        System.gc()
                        renderPageInternal(pageIndex, 1f)
                    } catch (e3: OutOfMemoryError) {
                         e3.printStackTrace()
                         null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun renderPageInternal(pageIndex: Int, scale: Float): Bitmap {
        val page = pdfRenderer!!.openPage(pageIndex)
        
        val width = (page.width * scale).toInt()
        val height = (page.height * scale).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val matrix = android.graphics.Matrix().apply {
            postScale(scale, scale)
        }
        
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    suspend fun close() {
        mutex.withLock {
            closeInternal()
        }
    }

    private fun closeInternal() {
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfRenderer = null
            fileDescriptor = null
            cachedPdfFile?.delete()
            cachedPdfFile = null
        }
    }
}
