package com.mefabz.scanner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val LANGUAGE_ACCENT_KEY = stringPreferencesKey("language_accent")
    private val IS_DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")
    private val SPEECH_RATE_KEY = floatPreferencesKey("speech_rate")
    private val INVOICE_PREFIXES_KEY = stringPreferencesKey("invoice_prefixes")

    val languageAccentFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE_ACCENT_KEY] ?: "en-US" // Default to US English
        }

    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_DARK_MODE_KEY] ?: true // Default to Dark mode
        }

    val speechRateFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[SPEECH_RATE_KEY] ?: 1.0f // Default to 1x speed
        }

    val invoicePrefixesFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[INVOICE_PREFIXES_KEY] ?: "MEFABZ" // Default to MEFABZ
        }

    suspend fun saveLanguageAccent(accentCode: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_ACCENT_KEY] = accentCode
        }
    }

    suspend fun saveThemeMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE_KEY] = isDark
        }
    }

    suspend fun saveSpeechRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[SPEECH_RATE_KEY] = rate
        }
    }

    suspend fun saveInvoicePrefixes(prefixes: String) {
        context.dataStore.edit { preferences ->
            preferences[INVOICE_PREFIXES_KEY] = prefixes
        }
    }
}
