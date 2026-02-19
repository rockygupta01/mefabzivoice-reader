package com.mefabz.scanner.domain.usecase

import com.mefabz.scanner.domain.model.NarrationPayload
import javax.inject.Inject

class BuildNarrationUseCase @Inject constructor() {
    operator fun invoke(products: List<String>, pageNumber: String): NarrationPayload {
        val narration = buildString {
            append("MEFABZ Invoice detected. Products listed are: ")
            products.forEach { append("$it, ") }
            append("Page ${pageNumber}.")
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
