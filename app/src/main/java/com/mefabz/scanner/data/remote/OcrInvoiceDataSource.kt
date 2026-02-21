package com.mefabz.scanner.data.remote

import android.graphics.BitmapFactory
import android.graphics.Rect
import com.mefabz.scanner.data.remote.dto.OcrInvoiceResponse
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.mefabz.scanner.data.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class OcrInvoiceDataSource @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {

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

        val prefixesString = userPreferencesRepository.invoicePrefixesFlow.first()
        val validPrefixes = parsePrefixes(prefixesString)

        val suffixesString = userPreferencesRepository.invoiceSuffixesFlow.first()
        val suffixRegex = buildSuffixRegex(suffixesString)

        val brandDetected = lines.any { line ->
            val normalizedLine = normalizedForBrand(line.text)
            validPrefixes.any { prefix -> normalizedLine.contains(normalizedForBrand(prefix)) }
        }

        return OcrInvoiceResponse(
            brandDetected = brandDetected,
            products = extractProductCandidates(lines, bitmap.height, validPrefixes, suffixRegex),
            pageNumber = extractFooterPageNumber(lines, bitmap.height).orEmpty()
        )
    }

    suspend fun parseInvoiceBitmap(bitmap: android.graphics.Bitmap): OcrInvoiceResponse {
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

        val prefixesString = userPreferencesRepository.invoicePrefixesFlow.first()
        val validPrefixes = parsePrefixes(prefixesString)

        val suffixesString = userPreferencesRepository.invoiceSuffixesFlow.first()
        val suffixRegex = buildSuffixRegex(suffixesString)

        val brandDetected = lines.any { line ->
            val normalizedLine = normalizedForBrand(line.text)
            validPrefixes.any { prefix -> normalizedLine.contains(normalizedForBrand(prefix)) }
        }

        return OcrInvoiceResponse(
            brandDetected = brandDetected,
            products = extractProductCandidates(lines, bitmap.height, validPrefixes, suffixRegex),
            pageNumber = extractFooterPageNumber(lines, bitmap.height).orEmpty()
        )
    }

    private fun parsePrefixes(raw: String): List<String> {
        return raw.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("MEFABZ") } // Fallback to MEFABZ if completely empty
    }

    private fun buildSuffixRegex(raw: String): Regex {
        val suffixes = raw.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            
        if (suffixes.isEmpty()) {
            return Regex("(?!)") // Matches nothing if completely empty
        }
        
        return Regex(
            suffixes.joinToString("|", prefix = "(?i)\\b(", postfix = ")\\b\\s*[)\\]]?")
        )
    }

    private fun extractProductCandidates(
        lines: List<RecognizedLine>, 
        imageHeight: Int, 
        validPrefixes: List<String>,
        suffixRegex: Regex
    ): List<String> {
        val topLimit = (imageHeight * 0.12f).toInt()
        val bottomLimit = (imageHeight * 0.88f).toInt()

        val validLines = lines.filter { line ->
            val centerY = line.box?.centerY() ?: (imageHeight / 2)
            centerY in topLimit..bottomLimit
        }

        val products = mutableListOf<String>()
        var i = 0

        while (i < validLines.size) {
            val lineText = validLines[i].text
            val upperLine = lineText.trim().uppercase()

            if (validPrefixes.any { prefix -> upperLine.startsWith(prefix) }) {
                var rawProductText = lineText
                
                // Check if this line already contains a color/suffix
                val hasColorAlready = suffixRegex.containsMatchIn(rawProductText)

                // If no color was found, peek at the next line
                if (!hasColorAlready && i + 1 < validLines.size) {
                    val nextLineText = validLines[i + 1].text.trim()
                    
                    if (suffixRegex.containsMatchIn(nextLineText)) {
                        // Color found on next line, append it
                        rawProductText = "$rawProductText $nextLineText"
                        i++ // Skip processing the next line as a standalone string
                    }
                }

                val cleaned = cleanProductCandidate(rawProductText, suffixRegex)
                if (cleaned.isNotBlank() && !isLikelyNonProductLine(cleaned)) {
                    products.add(cleaned)
                }
            }
            i++
        }

        return products.distinct()
    }

    private fun extractFooterPageNumber(lines: List<RecognizedLine>, imageHeight: Int): String? {
        // Look at bottom 20% of the page
        val footerStart = (imageHeight * 0.80f).toInt()
        
        // Prioritize lines at the bottom
        val footerLines = lines
            .filter { line -> (line.box?.centerY() ?: Int.MIN_VALUE) >= footerStart }
            .sortedByDescending { line -> line.box?.centerY() ?: 0 }

        for (line in footerLines) {
            val text = line.text.trim()
            
            // STRICT PAGE NUMBER RULE: Only ever a single number string
            if (text.matches(Regex("^\\d+$"))) {
                return text
            }
        }

        // Fallback: Check lines from bottom up if they are strictly digits
        return lines.asReversed()
            .map { it.text.trim() }
            .firstOrNull { it.matches(Regex("^\\d+$")) }
    }

    private fun cleanProductCandidate(raw: String, suffixRegex: Regex): String {
        var cleaned = raw
            .replace(Regex("^\\d+[.)-]?\\s*"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        val colorMatch = suffixRegex.find(cleaned)
        if (colorMatch != null) {
            cleaned = cleaned.substring(0, colorMatch.range.last + 1).trim()
        }

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
            "thank you",
            "sku",
            "qty",
            "quantity"
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
        
        // Strict price check for lines that are just numbers with decimals or currency symbols
        if (line.matches(Regex("^[\\$€₹£]?\\s*\\d+(?:[.,]\\d{1,2})?\\s*[\\$€₹£]?$"))) {
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
