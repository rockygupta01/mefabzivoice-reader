package com.mefabz.scanner.data.remote.dto

data class OcrInvoiceResponse(
    val brandDetected: Boolean,
    val products: List<String>,
    val pageNumber: String
)
