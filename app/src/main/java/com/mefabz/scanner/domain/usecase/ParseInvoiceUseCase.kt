package com.mefabz.scanner.domain.usecase

import com.mefabz.scanner.domain.model.ParseInvoiceResult
import com.mefabz.scanner.domain.repository.InvoiceRepository
import javax.inject.Inject

class ParseInvoiceUseCase @Inject constructor(
    private val invoiceRepository: InvoiceRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray): ParseInvoiceResult {
        return invoiceRepository.parseInvoice(imageBytes)
    }
}
