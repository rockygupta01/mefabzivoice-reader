package com.mefabz.scanner.domain.model

sealed interface ParseInvoiceResult {
    data class Success(val invoice: ParsedInvoice) : ParseInvoiceResult
    data class Error(val reason: InvoiceError) : ParseInvoiceResult
}
