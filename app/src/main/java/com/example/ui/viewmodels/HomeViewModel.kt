package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.LevelProgressEntity
import com.example.data.TrophyEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HomeUiState(
    val currentLevel: Int = 1,
    val completedLevelsCount: Int = 0,
    val totalStars: Int = 0,
    val isTodayDailyCompleted: Boolean = false,
    val unlockedTrophiesCount: Int = 0,
    val totalTrophiesCount: Int = 12,
    val allLevels: List<LevelProgressEntity> = emptyList(),
    val allTrophies: List<TrophyEntity> = emptyList()
)

class HomeViewModel(private val repository: GameRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeIfNeeded()

            launch {
                repository.allLevels.collect { levels ->
                    val unlocked = levels.filter { it.isUnlocked }.maxOfOrNull { it.levelId } ?: 1
                    val completed = levels.count { it.isCompleted }
                    val stars = levels.sumOf { it.stars }
                    _uiState.update {
                        it.copy(
                            currentLevel = unlocked,
                            completedLevelsCount = completed,
                            totalStars = stars,
                            allLevels = levels
                        )
                    }
                }
            }

            launch {
                repository.allTrophies.collect { trophies ->
                    val unlocked = trophies.count { it.isUnlocked }
                    _uiState.update {
                        it.copy(
                            unlockedTrophiesCount = unlocked,
                            totalTrophiesCount = trophies.size,
                            allTrophies = trophies
                        )
                    }
                }
            }

            checkTodayDaily()
        }
    }

    private fun checkTodayDaily() {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(Date())
            val challenge = repository.getDailyChallenge(todayStr)
            _uiState.update {
                it.copy(isTodayDailyCompleted = challenge?.isCompleted == true)
            }
        }
    }
}
