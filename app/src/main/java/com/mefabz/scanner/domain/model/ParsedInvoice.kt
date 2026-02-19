package com.mefabz.scanner.domain.model

data class ParsedInvoice(
    val products: List<String>,
    val pageNumber: String
)
