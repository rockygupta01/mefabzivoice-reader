package com.mefabz.scanner.domain.model

data class NarrationPayload(
    val narration: String,
    val productRanges: List<IntRange>
)
