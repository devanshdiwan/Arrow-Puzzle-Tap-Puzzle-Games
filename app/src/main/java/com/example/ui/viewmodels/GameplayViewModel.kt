package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdManager
import com.example.ads.RewardType
import com.example.data.GameRepository
import com.example.gameengine.LevelCatalog
import com.example.gameengine.LevelGenerator
import com.example.gameengine.PuzzleEngine
import com.example.gameengine.TapResult
import com.example.models.Arrow
import com.example.models.Difficulty
import com.example.models.GridPoint
import com.example.models.LevelDefinition
import com.example.utils.HapticManager
import com.example.utils.SoundEffectManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GameplayUiState(
    val levelNumber: Int = 1,
    val levelTitle: String = "Level 1",
    val difficulty: Difficulty = Difficulty.TUTORIAL,
    val levelDefinition: LevelDefinition? = null,
    val activeArrows: List<Arrow> = emptyList(),
    val lives: Int = 3,
    val maxLives: Int = 3,
    val movesCount: Int = 0,
    val hintsAvailable: Int = 3,
    val canUndo: Boolean = false,
    val isZoomMode: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    val isLevelCompleted: Boolean = false,
    val starsEarned: Int = 0,
    val elapsedSeconds: Long = 0L,
    val escapingArrow: Arrow? = null,
    val escapeProgress: Float = 0f,
    val shakingArrowId: String? = null,
    val hintArrowId: String? = null,
    val blockedCollisionPoints: List<GridPoint>? = null,
    val isDailyChallenge: Boolean = false
)

class GameplayViewModel(
    private val repository: GameRepository,
    val soundManager: SoundEffectManager,
    val hapticManager: HapticManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameplayUiState())
    val uiState: StateFlow<GameplayUiState> = _uiState.asStateFlow()

    private var engine: PuzzleEngine? = null
    private var timerJob: Job? = null
    private var escapeAnimJob: Job? = null

    init {
        // Collect user sound/music/vibration preferences
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                soundManager.isSoundEnabled = settings.soundEnabled
                soundManager.isMusicEnabled = settings.musicEnabled
                hapticManager.isVibrationEnabled = settings.vibrationEnabled
                _uiState.update { it.copy(hintsAvailable = settings.hintsCount) }
            }
        }
    }

    fun startLevel(levelNumber: Int) {
        val def = LevelCatalog.getLevel(levelNumber)
        setupEngine(def, isDaily = false)
    }

    fun startDailyChallenge() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        // Generate daily puzzle seeded by today's date
        val dailySeed = todayStr.hashCode().toLong()
        val def = LevelGenerator.generateSolvableLevel(
            levelNumber = 101, // daily identifier
            cols = 7,
            rows = 8,
            targetArrowCount = 22,
            allowBends = true,
            silhouette = LevelGenerator.Silhouette.HEART,
            seed = dailySeed
        )
        setupEngine(def, isDaily = true, customTitle = "Daily Challenge")
    }

    private fun setupEngine(def: LevelDefinition, isDaily: Boolean, customTitle: String? = null) {
        val newEngine = PuzzleEngine(def)
        engine = newEngine

        _uiState.value = GameplayUiState(
            levelNumber = def.levelNumber,
            levelTitle = customTitle ?: "Level ${def.levelNumber}",
            difficulty = def.difficulty,
            levelDefinition = def,
            activeArrows = newEngine.activeArrows,
            lives = newEngine.lives,
            maxLives = newEngine.maxLives,
            movesCount = 0,
            hintsAvailable = _uiState.value.hintsAvailable,
            canUndo = false,
            isZoomMode = false,
            isPaused = false,
            isGameOver = false,
            isLevelCompleted = false,
            isDailyChallenge = isDaily
        )

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (!_uiState.value.isPaused && !_uiState.value.isGameOver && !_uiState.value.isLevelCompleted) {
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    fun onArrowTapped(arrowId: String) {
        val currentEngine = engine ?: return
        if (_uiState.value.isPaused || _uiState.value.isGameOver || _uiState.value.isLevelCompleted) return
        if (_uiState.value.escapingArrow != null) return // animation in progress

        val result = currentEngine.tapArrow(arrowId)
        when (result) {
            is TapResult.Success -> {
                soundManager.playEscape()
                hapticManager.vibrateTap()

                // Trigger escape projectile animation
                escapeAnimJob?.cancel()
                escapeAnimJob = viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            escapingArrow = result.arrow,
                            escapeProgress = 0f,
                            hintArrowId = null,
                            blockedCollisionPoints = null
                        )
                    }

                    // Animate escape progress from 0f to 1f over 180ms
                    val steps = 8
                    for (step in 1..steps) {
                        delay(20)
                        _uiState.update { it.copy(escapeProgress = step.toFloat() / steps) }
                    }

                    _uiState.update {
                        it.copy(
                            escapingArrow = null,
                            escapeProgress = 0f,
                            activeArrows = currentEngine.activeArrows,
                            movesCount = currentEngine.movesCount,
                            canUndo = currentEngine.canUndo
                        )
                    }

                    if (result.isLevelComplete) {
                        onLevelWon()
                    }
                }
            }

            is TapResult.Blocked -> {
                soundManager.playBlocked()
                hapticManager.vibrateBlocked()

                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            shakingArrowId = arrowId,
                            blockedCollisionPoints = result.pathPoints,
                            lives = result.remainingLives,
                            isGameOver = result.isGameOver
                        )
                    }
                    delay(350)
                    _uiState.update {
                        it.copy(
                            shakingArrowId = null,
                            blockedCollisionPoints = null
                        )
                    }
                }
            }

            TapResult.Ignored -> {}
        }
    }

    private fun onLevelWon() {
        val currentEngine = engine ?: return
        val stars = currentEngine.calculateStars()
        soundManager.playVictory()
        hapticManager.vibrateVictory()

        _uiState.update {
            it.copy(
                isLevelCompleted = true,
                starsEarned = stars
            )
        }

        viewModelScope.launch {
            if (_uiState.value.isDailyChallenge) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = dateFormat.format(Date())
                repository.completeDailyChallenge(todayStr, stars)
            } else {
                repository.completeLevel(
                    levelId = _uiState.value.levelNumber,
                    stars = stars,
                    moves = currentEngine.movesCount,
                    timeSeconds = _uiState.value.elapsedSeconds,
                    hintsUsed = currentEngine.hintsUsed,
                    livesRemaining = currentEngine.lives
                )
            }
        }
    }

    fun undo() {
        val currentEngine = engine ?: return
        if (currentEngine.undo()) {
            soundManager.playTap()
            hapticManager.vibrateTap()
            viewModelScope.launch {
                repository.unlockTrophy("undo_tactician")
            }
            _uiState.update {
                it.copy(
                    activeArrows = currentEngine.activeArrows,
                    lives = currentEngine.lives,
                    movesCount = currentEngine.movesCount,
                    canUndo = currentEngine.canUndo,
                    hintArrowId = null,
                    blockedCollisionPoints = null
                )
            }
        }
    }

    fun useHint() {
        val currentEngine = engine ?: return
        viewModelScope.launch {
            if (repository.useHint()) {
                val hint = currentEngine.getHintArrow()
                if (hint != null) {
                    soundManager.playTap()
                    hapticManager.vibrateTap()
                    _uiState.update { it.copy(hintArrowId = hint.id) }
                }
            } else {
                // Out of hints: prompt rewarded ad
                AdManager.showRewardedAd(
                    title = "Gain +1 Hint",
                    rewardType = RewardType.EXTRA_HINT
                ) { earned ->
                    if (earned) {
                        viewModelScope.launch {
                            repository.addHints(1)
                            val hint = currentEngine.getHintArrow()
                            _uiState.update { it.copy(hintArrowId = hint?.id) }
                        }
                    }
                }
            }
        }
    }

    fun toggleZoom() {
        soundManager.playTap()
        _uiState.update { it.copy(isZoomMode = !it.isZoomMode) }
    }

    fun pause() {
        soundManager.playTap()
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resume() {
        soundManager.playTap()
        _uiState.update { it.copy(isPaused = false) }
    }

    fun restart() {
        soundManager.playTap()
        val def = _uiState.value.levelDefinition ?: return
        setupEngine(def, _uiState.value.isDailyChallenge, _uiState.value.levelTitle)
    }

    fun watchAdForLife() {
        AdManager.showRewardedAd(
            title = "Gain +1 Life",
            rewardType = RewardType.EXTRA_LIFE
        ) { earned ->
            if (earned) {
                val currentEngine = engine ?: return@showRewardedAd
                currentEngine.addLife(1)
                _uiState.update {
                    it.copy(
                        lives = currentEngine.lives,
                        isGameOver = false
                    )
                }
            }
        }
    }

    fun nextLevel() {
        soundManager.playTap()
        val next = _uiState.value.levelNumber + 1
        if (next <= LevelCatalog.TOTAL_LEVELS) {
            startLevel(next)
        }
    }

    fun setSound(enabled: Boolean) {
        viewModelScope.launch { repository.setSound(enabled) }
    }

    fun setMusic(enabled: Boolean) {
        viewModelScope.launch { repository.setMusic(enabled) }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch { repository.setVibration(enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        escapeAnimJob?.cancel()
    }
}
