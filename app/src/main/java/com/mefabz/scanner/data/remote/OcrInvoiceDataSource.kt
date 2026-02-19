package com.mefabz.scanner.data.remote

import android.graphics.BitmapFactory
import android.graphics.Rect
import com.mefabz.scanner.data.remote.dto.OcrInvoiceResponse
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class OcrInvoiceDataSource @Inject constructor() {

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun parseInvoice(imageBytes: ByteArray): OcrInvoiceResponse {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalStateException("Unable to decode captured invoice image")

        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizer.processAwait(image)

        val lines = visionText.textBlocks
            .flatMap { block -> block.lines.map { line -> RecognizedLine(line.text, line.boundingBox) } }
            .map { line ->
                line.copy(
                    text = line.text
                        .replace(Regex("\\s+"), " ")
                        .trim()
                )
            }
            .filter { it.text.isNotBlank() }

        if (lines.isEmpty()) {
            throw IllegalStateException("No text found in invoice image")
        }

        val brandDetected = lines.any { normalizedForBrand(it.text).contains("mefabz") }

        return OcrInvoiceResponse(
            brandDetected = brandDetected,
            products = extractProductCandidates(lines, bitmap.height),
            pageNumber = extractFooterPageNumber(lines, bitmap.height).orEmpty()
        )
    }

    private fun extractProductCandidates(lines: List<RecognizedLine>, imageHeight: Int): List<String> {
        val topLimit = (imageHeight * 0.12f).toInt()
        val bottomLimit = (imageHeight * 0.88f).toInt()

        return lines
            .asSequence()
            .filter { line ->
                val centerY = line.box?.centerY() ?: (imageHeight / 2)
                centerY in topLimit..bottomLimit
            }
            .map { cleanProductCandidate(it.text) }
            .filter(String::isNotBlank)
            .filterNot(::isLikelyNonProductLine)
            .distinct()
            .toList()
    }

    private fun extractFooterPageNumber(lines: List<RecognizedLine>, imageHeight: Int): String? {
        val footerStart = (imageHeight * 0.82f).toInt()
        val strictPageNumberPattern = Regex("^\\d+$")

        val footerCandidate = lines
            .filter { line -> (line.box?.centerY() ?: Int.MIN_VALUE) >= footerStart }
            .map { it.text.trim() }
            .firstOrNull { strictPageNumberPattern.matches(it) }

        if (footerCandidate != null) {
            return footerCandidate
        }

        return lines
            .asReversed()
            .map { it.text.trim() }
            .firstOrNull { strictPageNumberPattern.matches(it) }
    }

    private fun cleanProductCandidate(raw: String): String {
        var cleaned = raw
            .replace(Regex("^\\d+[.)-]?\\s*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val currencyIndex = cleaned.indexOfFirst { it == '$' || it == '₹' || it == '€' }
        if (currencyIndex > 0) {
            cleaned = cleaned.substring(0, currencyIndex).trim()
        }

        cleaned = cleaned.replace(
            Regex("\\b(?:qty|qnty|quantity|sku|hsn|tax|vat|gst)\\b.*", RegexOption.IGNORE_CASE),
            ""
        ).trim()

        cleaned = stripTrailingNumericColumns(cleaned)

        return cleaned
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun stripTrailingNumericColumns(line: String): String {
        val tokens = line.split(' ').filter(String::isNotBlank).toMutableList()
        if (tokens.isEmpty()) return ""

        var numericTailCount = 0
        for (index in tokens.lastIndex downTo 0) {
            if (tokens[index].isNumericColumnToken()) {
                numericTailCount++
            } else {
                break
            }
        }

        if (numericTailCount >= 2) {
            repeat(numericTailCount) {
                tokens.removeAt(tokens.lastIndex)
            }
        }

        return tokens.joinToString(" ")
    }

    private fun String.isNumericColumnToken(): Boolean {
        val normalized = replace(",", "")
        return normalized.matches(Regex("^[xX]?\\d+(?:\\.\\d+)?%?$")) ||
            normalized.matches(Regex("^\\d+[xX]$"))
    }

    private fun isLikelyNonProductLine(line: String): Boolean {
        if (line.length < 3) return true
        if (!line.any(Char::isLetter)) return true

        val lower = line.lowercase()
        val blockedPhrases = listOf(
            "invoice",
            "inv no",
            "bill to",
            "ship to",
            "subtotal",
            "total",
            "tax",
            "vat",
            "gst",
            "amount",
            "balance",
            "date",
            "phone",
            "email",
            "address",
            "page",
            "terms",
            "payment",
            "thank you"
        )
        if (blockedPhrases.any(lower::contains)) {
            return true
        }

        if (line.matches(Regex("^\\d+$"))) {
            return true
        }

        if (Regex("""\b\d{1,2}[-/]\d{1,2}[-/]\d{2,4}\b""").containsMatchIn(line)) {
            return true
        }

        val digitCount = line.count(Char::isDigit)
        return digitCount >= (line.length / 2)
    }

    private fun normalizedForBrand(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }

    private suspend fun TextRecognizer.processAwait(image: InputImage): Text {
        return suspendCancellableCoroutine { continuation ->
            process(image)
                .addOnSuccessListener { text ->
                    if (continuation.isActive) {
                        continuation.resume(text)
                    }
                }
                .addOnFailureListener { throwable ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(throwable)
                    }
                }
        }
    }

    private data class RecognizedLine(
        val text: String,
        val box: Rect?
    )
}
