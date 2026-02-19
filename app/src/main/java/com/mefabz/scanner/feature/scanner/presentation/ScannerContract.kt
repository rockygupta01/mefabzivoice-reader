package com.mefabz.scanner.feature.scanner.presentation

import com.mefabz.scanner.domain.model.InvoiceError

sealed interface ScanState {
    data object Idle : ScanState
    data object Capturing : ScanState
    data object Processing : ScanState
    data object Parsed : ScanState
    data class Error(val reason: InvoiceError) : ScanState
}

sealed interface SpeechState {
    data object Idle : SpeechState
    data class Speaking(val activeProductIndex: Int?) : SpeechState
    data object Done : SpeechState
    data class Error(val message: String) : SpeechState
}

data class InvoiceUiModel(
    val products: List<String>,
    val pageNumber: String,
    val imageUri: String
)

data class ScannerUiState(
    val scanState: ScanState = ScanState.Idle,
    val speechState: SpeechState = SpeechState.Idle,
    val parsedInvoice: InvoiceUiModel? = null
)

sealed interface ScannerEvent {
    data object NavigateToResult : ScannerEvent
}
