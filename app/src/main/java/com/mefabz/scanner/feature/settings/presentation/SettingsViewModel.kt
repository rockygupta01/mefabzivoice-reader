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

    fun onLanguageAccentChanged(newAccentCode: String) {
        viewModelScope.launch {
            userPreferencesRepository.saveLanguageAccent(newAccentCode)
        }
    }
}
