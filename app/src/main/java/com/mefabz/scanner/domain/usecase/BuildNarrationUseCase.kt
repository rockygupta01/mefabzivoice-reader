package com.mefabz.scanner.domain.usecase

import com.mefabz.scanner.domain.model.NarrationPayload
import com.mefabz.scanner.feature.scanner.presentation.components.extractColorsFromProducts
import javax.inject.Inject

class BuildNarrationUseCase @Inject constructor() {
    operator fun invoke(products: List<String>, pageNumber: String): NarrationPayload {
        val colors = extractColorsFromProducts(products)
        val narration = buildString {
            append("MEFABZ Invoice detected. Products listed are: ")
            products.forEach { append("$it, ") }
            append("Page ${pageNumber}.")
            if (colors.isNotEmpty()) {
                append(" Color ${colors.joinToString(", ")}.")
            }
        }

        var cursor = 0
        val ranges = products.mapNotNull { product ->
            val start = narration.indexOf(product, startIndex = cursor)
            if (start == -1) {
                null
            } else {
                val endExclusive = start + product.length
                cursor = endExclusive
                start until endExclusive
            }
        }

        return NarrationPayload(
            narration = narration,
            productRanges = ranges
        )
    }
}
