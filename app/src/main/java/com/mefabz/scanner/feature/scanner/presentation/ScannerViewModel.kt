package com.mefabz.scanner.feature.scanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mefabz.scanner.core.dispatcher.IoDispatcher
import com.mefabz.scanner.core.tts.GoogleTtsManager
import com.mefabz.scanner.core.tts.TtsListener
import com.mefabz.scanner.data.repository.UserPreferencesRepository
import com.mefabz.scanner.domain.model.InvoiceError
import com.mefabz.scanner.domain.model.ParseInvoiceResult
import com.mefabz.scanner.domain.usecase.BuildNarrationUseCase
import com.mefabz.scanner.domain.usecase.ParseInvoiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val parseInvoiceUseCase: ParseInvoiceUseCase,
    private val buildNarrationUseCase: BuildNarrationUseCase,
    private val ttsManager: GoogleTtsManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel(), TtsListener {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ScannerEvent>()
    val events: SharedFlow<ScannerEvent> = _events.asSharedFlow()

    private val validPrefixesFlow = userPreferencesRepository.invoicePrefixesFlow
        .map { raw -> raw.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.ifEmpty { listOf("MEFABZ") } }

    private var activeNarrationRanges: List<IntRange> = emptyList()

    init {
        ttsManager.setListener(this)
    }

    private var scanJob: kotlinx.coroutines.Job? = null
    
    fun onCaptureStarted() {
        _uiState.update { it.copy(scanState = ScanState.Capturing) }
    }

    fun onCaptureFailed() {
        _uiState.update { it.copy(scanState = ScanState.Error(InvoiceError.BlurryInvoice)) }
    }

    fun processCapturedInvoice(imageBytes: ByteArray, imageUri: String) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch(ioDispatcher) {
            _uiState.update {
                it.copy(
                    scanState = ScanState.Processing,
                    speechState = SpeechState.Idle
                )
            }

            when (val result = parseInvoiceUseCase(imageBytes)) {
                is ParseInvoiceResult.Success -> {
                    val narrationResult = buildNarrationUseCase(
                        result.invoice.products,
                        result.invoice.pageNumber
                    )
                    _uiState.update {
                        it.copy(
                            scanState = ScanState.Parsed,
                            parsedInvoice = InvoiceUiModel(
                                products = result.invoice.products,
                                pageNumber = result.invoice.pageNumber,
                                imageUri = imageUri
                            )
                        )
                    }
                    _events.emit(ScannerEvent.NavigateToResult)
                }

                is ParseInvoiceResult.Error -> {
                    _uiState.update {
                        it.copy(scanState = ScanState.Error(result.reason))
                    }
                }
            }
        }
    }

    fun startNarration() {
        val invoice = _uiState.value.parsedInvoice ?: return
        val narrationPayload = buildNarrationUseCase(invoice.products, invoice.pageNumber)
        activeNarrationRanges = narrationPayload.productRanges

        _uiState.update { it.copy(speechState = SpeechState.Speaking(activeProductIndex = null)) }

        viewModelScope.launch {
            val languageCode = userPreferencesRepository.languageAccentFlow.first()
            ttsManager.setLanguage(languageCode)
            ttsManager.speak(narrationPayload.narration)
        }
    }

    fun stopNarration() {
        ttsManager.stop()
        _uiState.update { it.copy(speechState = SpeechState.Idle) }
    }

    fun resetForRescan() {
        scanJob?.cancel()
        stopNarration()
        activeNarrationRanges = emptyList()
        _uiState.value = ScannerUiState()
    }

    override fun onStart(utteranceId: String) {
        _uiState.update { it.copy(speechState = SpeechState.Speaking(activeProductIndex = null)) }
    }

    override fun onDone(utteranceId: String) {
        _uiState.update { it.copy(speechState = SpeechState.Done) }
    }

    override fun onError(utteranceId: String) {
        _uiState.update {
            it.copy(speechState = SpeechState.Error("Text-to-speech playback failed"))
        }
    }

    override fun onRangeStart(utteranceId: String, start: Int, end: Int) {
        val currentIndex = activeNarrationRanges.indexOfFirst { start in it || end in it }
        if (currentIndex >= 0) {
            _uiState.update { it.copy(speechState = SpeechState.Speaking(activeProductIndex = currentIndex)) }
        }
    }

    fun errorLabel(reason: InvoiceError): String {
        return when (reason) {
            InvoiceError.NonMefabz -> "This is not a genuine invoice (Prefix missing)"
            InvoiceError.NoProducts -> "No valid products found on this invoice"
            InvoiceError.BlurryInvoice -> "Invoice seems blurry. Please capture again"
            is InvoiceError.ApiFailure -> "Invoice parsing failed: ${reason.message}"
        }
    }

    override fun onCleared() {
        ttsManager.setListener(null)
        ttsManager.shutdown()
    }
}
