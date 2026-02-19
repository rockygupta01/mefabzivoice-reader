package com.mefabz.scanner.domain.repository

import com.mefabz.scanner.domain.model.ParseInvoiceResult

interface InvoiceRepository {
    suspend fun parseInvoice(imageBytes: ByteArray): ParseInvoiceResult
}
