package com.mefabz.scanner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    val languageAccentFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE_ACCENT_KEY] ?: "en-US" // Default to US English
        }

    suspend fun saveLanguageAccent(accentCode: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_ACCENT_KEY] = accentCode
        }
    }
}
