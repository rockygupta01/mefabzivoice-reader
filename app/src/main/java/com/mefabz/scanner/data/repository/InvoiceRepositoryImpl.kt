package com.mefabz.scanner.data.repository

import com.mefabz.scanner.data.remote.OcrInvoiceDataSource
import com.mefabz.scanner.domain.model.InvoiceError
import com.mefabz.scanner.domain.model.ParseInvoiceResult
import com.mefabz.scanner.domain.model.ParsedInvoice
import com.mefabz.scanner.domain.repository.InvoiceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepositoryImpl @Inject constructor(
    private val dataSource: OcrInvoiceDataSource
) : InvoiceRepository {

    override suspend fun parseInvoice(imageBytes: ByteArray): ParseInvoiceResult {
        return try {
            val response = dataSource.parseInvoice(imageBytes)
            internalParse(response)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override suspend fun parseInvoice(bitmap: android.graphics.Bitmap): ParseInvoiceResult {
        return try {
            val response = dataSource.parseInvoiceBitmap(bitmap)
            internalParse(response)
        } catch (e: Exception) {
            handleError(e)
        }
    }

    private fun internalParse(response: com.mefabz.scanner.data.remote.dto.OcrInvoiceResponse): ParseInvoiceResult {
        if (!response.brandDetected) {
            return ParseInvoiceResult.Error(InvoiceError.NonMefabz)
        }

        val products = response.products
            .map(::cleanProduct)
            .filter(String::isNotBlank)
            .filterNot(::looksLikeNonProductLine)

        if (products.isEmpty()) {
            return ParseInvoiceResult.Error(InvoiceError.NoProducts)
        }

        val pageNumber = sanitizePageNumber(response.pageNumber)
            ?: return ParseInvoiceResult.Error(InvoiceError.BlurryInvoice)

        return ParseInvoiceResult.Success(
            ParsedInvoice(
                products = products,
                pageNumber = pageNumber
            )
        )
    }

    private fun handleError(exception: Exception): ParseInvoiceResult {
        val message = exception.message.orEmpty()
        return if (
            message.contains("blur", ignoreCase = true) ||
            message.contains("no text found", ignoreCase = true)
        ) {
            ParseInvoiceResult.Error(InvoiceError.BlurryInvoice)
        } else {
            ParseInvoiceResult.Error(
                InvoiceError.ApiFailure(
                    message = if (message.isBlank()) {
                        "Failed to parse invoice"
                    } else {
                        message
                    }
                )
            )
        }
    }

    private fun cleanProduct(raw: String): String {
        return raw
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(' ')
            .joinToString(" ") { token ->
                if (token.length <= 2) token.uppercase() else token.replaceFirstChar { it.uppercase() }
            }
    }

    private fun looksLikeNonProductLine(line: String): Boolean {
        val lower = line.lowercase()
        val forbiddenWords = listOf(
            "subtotal",
            "total",
            "tax",
            "vat",
            "invoice",
            "date",
            "qty",
            "quantity",
            "sku",
            "amount"
        )
        if (forbiddenWords.any(lower::contains)) {
            return true
        }

        val priceRegex = Regex("""[$€₹]\s*\d""")
        val dateRegex = Regex("""\b\d{1,2}[-/]\d{1,2}[-/]\d{2,4}\b""")
        return priceRegex.containsMatchIn(line) || dateRegex.containsMatchIn(line)
    }

    private fun sanitizePageNumber(raw: String): String? {
        val trimmed = raw.trim()
        // STRICT RULE: Must be only digits
        if (trimmed.matches(Regex("^\\d+$"))) {
            return trimmed
        }
        return null
    }
}
