package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "arrow_puzzle_settings")

data class GameSettings(
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val hintsCount: Int = 3
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val KEY_SOUND = booleanPreferencesKey("pref_sound_enabled")
        val KEY_MUSIC = booleanPreferencesKey("pref_music_enabled")
        val KEY_VIBRATION = booleanPreferencesKey("pref_vibration_enabled")
        val KEY_HINTS = intPreferencesKey("pref_hints_count")
    }

    val settingsFlow: Flow<GameSettings> = context.dataStore.data.map { preferences ->
        GameSettings(
            soundEnabled = preferences[PreferencesKeys.KEY_SOUND] ?: true,
            musicEnabled = preferences[PreferencesKeys.KEY_MUSIC] ?: true,
            vibrationEnabled = preferences[PreferencesKeys.KEY_VIBRATION] ?: true,
            hintsCount = preferences[PreferencesKeys.KEY_HINTS] ?: 3
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_SOUND] = enabled
        }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MUSIC] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_VIBRATION] = enabled
        }
    }

    suspend fun addHints(amount: Int) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.KEY_HINTS] ?: 3
            preferences[PreferencesKeys.KEY_HINTS] = (current + amount).coerceAtLeast(0)
        }
    }

    suspend fun useHint(): Boolean {
        var used = false
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.KEY_HINTS] ?: 3
            if (current > 0) {
                preferences[PreferencesKeys.KEY_HINTS] = current - 1
                used = true
            }
        }
        return used
    }
}
