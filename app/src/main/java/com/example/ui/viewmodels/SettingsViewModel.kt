package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.GameSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: GameRepository) : ViewModel() {

    val settings: StateFlow<GameSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameSettings()
    )

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch { repository.setSound(enabled) }
    }

    fun toggleMusic(enabled: Boolean) {
        viewModelScope.launch { repository.setMusic(enabled) }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch { repository.setVibration(enabled) }
    }

    fun resetAllProgress() {
        viewModelScope.launch { repository.resetAllProgress() }
    }
}
