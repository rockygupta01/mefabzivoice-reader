package com.mefabz.scanner.domain.model

sealed interface InvoiceError {
    data object NonMefabz : InvoiceError
    data object NoProducts : InvoiceError
    data object BlurryInvoice : InvoiceError
    data class ApiFailure(val message: String) : InvoiceError
}
