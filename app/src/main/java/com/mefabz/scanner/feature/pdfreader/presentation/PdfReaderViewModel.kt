package com.mefabz.scanner.feature.pdfreader.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mefabz.scanner.core.tts.GoogleTtsManager
import com.mefabz.scanner.core.tts.TtsListener
import com.mefabz.scanner.domain.model.InvoiceError
import com.mefabz.scanner.domain.model.ParseInvoiceResult
import com.mefabz.scanner.domain.repository.InvoiceRepository
import com.mefabz.scanner.domain.usecase.BuildNarrationUseCase
import com.mefabz.scanner.feature.pdfreader.domain.PdfContentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PdfReaderUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val pageBitmap: Bitmap? = null,
    val isReading: Boolean = false,
    val extractedText: String? = null,
    val detectedProducts: List<String> = emptyList(),
    val detectedInvoicePage: String? = null,
    val activeProductIndex: Int? = null
)

@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    private val pdfContentManager: PdfContentManager,
    private val ttsManager: GoogleTtsManager,
    private val invoiceRepository: InvoiceRepository,
    private val buildNarrationUseCase: BuildNarrationUseCase
) : ViewModel(), TtsListener {

    private val _uiState = MutableStateFlow(PdfReaderUiState())
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

    private var currentUri: String? = null
    private var pageLoadJob: Job? = null
    private val pageParseCache = mutableMapOf<Int, ParseInvoiceResult>()
    private var activeNarrationRanges: List<IntRange> = emptyList()

    init {
        ttsManager.setListener(this)
    }

    fun loadPdf(uri: String) {
        if (currentUri == uri && _uiState.value.pageCount > 0) return
        currentUri = uri
        pageParseCache.clear()
        pageLoadJob?.cancel()
        ttsManager.stop()
        activeNarrationRanges = emptyList()

        pageLoadJob = viewModelScope.launch {
            val staleBitmap = _uiState.value.pageBitmap
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentPage = 0,
                    pageCount = 0,
                    pageBitmap = null,
                    isReading = false,
                    extractedText = null,
                    detectedProducts = emptyList(),
                    detectedInvoicePage = null,
                    activeProductIndex = null
                )
            }
            if (staleBitmap != null && !staleBitmap.isRecycled) {
                staleBitmap.recycle()
            }

            val opened = runCatching {
                withContext(Dispatchers.IO) {
                    pdfContentManager.openPdf(uri)
                }
            }

            if (opened.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to open PDF: ${opened.exceptionOrNull()?.message}"
                    )
                }
                return@launch
            }

            val pageCount = pdfContentManager.pageCount
            if (pageCount <= 0) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "The selected PDF has no pages"
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(pageCount = pageCount) }

            // Try to find first MEFABZ page or default to 0
            val startPage = withContext(Dispatchers.IO) {
                findFirstMefabzPage(pageCount)
            }

            if (startPage != null) {
                loadPageInternal(startPage, autoSpeak = false) // Don't auto-speak on load, user must preview first
            } else {
                loadPageInternal(0, autoSpeak = false)
            }
        }
    }

    fun nextPage() {
        val currentState = _uiState.value
        if (currentState.currentPage < currentState.pageCount - 1) {
            loadPage(currentState.currentPage + 1)
        }
    }

    fun previousPage() {
        val currentState = _uiState.value
        if (currentState.currentPage > 0) {
            loadPage(currentState.currentPage - 1)
        }
    }

    private fun loadPage(pageIndex: Int) {
        pageLoadJob?.cancel()
        pageLoadJob = viewModelScope.launch {
            loadPageInternal(pageIndex, autoSpeak = false)
        }
    }

    private suspend fun loadPageInternal(pageIndex: Int, autoSpeak: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = true,
                isReading = false,
                error = null,
                extractedText = null,
                detectedProducts = emptyList(),
                detectedInvoicePage = null,
                activeProductIndex = null
            )
        }
        ttsManager.stop()
        activeNarrationRanges = emptyList()

        val bitmap = withContext(Dispatchers.IO) {
            pdfContentManager.renderPage(pageIndex)
        }

        if (bitmap == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Failed to render PDF page"
                )
            }
            return
        }

        val previousBitmap = _uiState.value.pageBitmap
        _uiState.update { it.copy(currentPage = pageIndex, pageBitmap = bitmap) }
        if (previousBitmap != null && previousBitmap != bitmap && !previousBitmap.isRecycled) {
            previousBitmap.recycle()
        }

        // Perform OCR silently to prepare text
        val result = withContext(Dispatchers.IO) {
            parsePage(pageIndex, bitmap)
        }

        when (result) {
            is ParseInvoiceResult.Success -> {
                val narrationPayload = buildNarrationUseCase(
                    products = result.invoice.products,
                    pageNumber = result.invoice.pageNumber
                )
                activeNarrationRanges = narrationPayload.productRanges
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        extractedText = narrationPayload.narration,
                        detectedProducts = result.invoice.products,
                        detectedInvoicePage = result.invoice.pageNumber,
                        activeProductIndex = null
                    )
                }
                if (autoSpeak) {
                    speak(narrationPayload.narration)
                }
            }

            is ParseInvoiceResult.Error -> {
                activeNarrationRanges = emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        extractedText = null,
                        detectedProducts = emptyList(),
                        detectedInvoicePage = null,
                        activeProductIndex = null,
                        error = errorText(result.reason)
                    )
                }
            }
        }
    }

    private suspend fun parsePage(pageIndex: Int, bitmap: Bitmap): ParseInvoiceResult {
        pageParseCache[pageIndex]?.let { return it }
        val result = invoiceRepository.parseInvoice(bitmap)
        pageParseCache[pageIndex] = result
        return result
    }

    private suspend fun findFirstMefabzPage(pageCount: Int): Int? {
        // Quick scan of first 3 pages then give up to avoid long loading
        val maxScan = minOf(pageCount, 3)
        for (index in 0 until maxScan) {
            val bitmap = pdfContentManager.renderPage(index) ?: continue
            try {
                val result = parsePage(index, bitmap)
                if (result is ParseInvoiceResult.Success) {
                    return index
                }
            } finally {
                // Don't recycle here because it might be cached in pageParseCache if we used it?
                // Actually PdfContentManager creates a new bitmap every time. 
                // We should recycle if we are not using it.
                // But parsePage stores the result, not the bitmap.
                bitmap.recycle()
            }
        }
        return null
    }

    fun toggleRead() {
        val state = _uiState.value
        if (state.isReading) {
            ttsManager.stop()
            _uiState.update { it.copy(isReading = false, activeProductIndex = null) }
        } else {
            state.extractedText?.let { text ->
                _uiState.update { it.copy(activeProductIndex = null) }
                speak(text)
            }
        }
    }

    private fun speak(text: String) {
        ttsManager.speak(text, PDF_UTTERANCE_ID)
    }

    override fun onStart(utteranceId: String) {
        if (utteranceId != PDF_UTTERANCE_ID) return
        _uiState.update { it.copy(isReading = true, activeProductIndex = null) }
    }

    override fun onDone(utteranceId: String) {
        if (utteranceId != PDF_UTTERANCE_ID) return
        _uiState.update { it.copy(isReading = false, activeProductIndex = null) }
    }

    override fun onError(utteranceId: String) {
        if (utteranceId != PDF_UTTERANCE_ID) return
        _uiState.update {
            it.copy(
                isReading = false,
                activeProductIndex = null,
                error = "Text-to-speech playback failed"
            )
        }
    }

    override fun onRangeStart(utteranceId: String, start: Int, end: Int) {
        if (utteranceId != PDF_UTTERANCE_ID) return
        val currentIndex = activeNarrationRanges.indexOfFirst { range ->
            start in range || (end > 0 && end - 1 in range) || end in range
        }
        if (currentIndex >= 0) {
            _uiState.update { it.copy(activeProductIndex = currentIndex) }
        }
    }

    private fun errorText(error: InvoiceError): String {
        return when (error) {
            InvoiceError.NonMefabz -> "This page is not a MEFABZ invoice page"
            InvoiceError.NoProducts -> "No valid product descriptions found on this page"
            InvoiceError.BlurryInvoice -> "Could not read page number from footer"
            is InvoiceError.ApiFailure -> "Parsing failed: ${error.message}"
        }
    }

    override fun onCleared() {
        pageLoadJob?.cancel()
        releaseCurrentBitmap()
        ttsManager.stop()
        ttsManager.setListener(null)
        viewModelScope.launch(Dispatchers.IO) {
            pdfContentManager.close()
        }
    }

    private fun releaseCurrentBitmap() {
        val bitmap = _uiState.value.pageBitmap
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }

    private companion object {
        const val PDF_UTTERANCE_ID = "pdf_reader_utterance"
    }
}
