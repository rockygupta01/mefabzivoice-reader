package com.mefabz.scanner.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mefabz.scanner.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val currentLanguageAccent: StateFlow<String> = userPreferencesRepository.languageAccentFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en-US" // Default backup
        )

    val isDarkMode: StateFlow<Boolean> = userPreferencesRepository.isDarkModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true // Default to Dark mode
        )

    val speechRate: StateFlow<Float> = userPreferencesRepository.speechRateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1.0f
        )

    val invoicePrefixes: StateFlow<String> = userPreferencesRepository.invoicePrefixesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val invoiceSuffixes: StateFlow<String> = userPreferencesRepository.invoiceSuffixesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun onThemeChanged(isDark: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.saveThemeMode(isDark)
        }
    }

    fun onLanguageAccentChanged(newAccentCode: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveLanguageAccent(newAccentCode)
        }
    }

    fun onSpeechRateChanged(rate: Float) {
        viewModelScope.launch {
            userPreferencesRepository.saveSpeechRate(rate)
        }
    }

    fun onInvoicePrefixesChanged(prefixes: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveInvoicePrefixes(prefixes)
        }
    }

    fun onInvoiceSuffixesChanged(suffixes: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveInvoiceSuffixes(suffixes)
        }
    }
}
