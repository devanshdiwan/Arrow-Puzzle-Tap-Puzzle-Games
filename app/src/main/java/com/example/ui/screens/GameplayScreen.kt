package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ads.AdManager
import com.example.ui.components.ActionControls
import com.example.ui.components.ArrowBoardCanvas
import com.example.ui.components.GameOverDialog
import com.example.ui.components.LevelCompletedDialog
import com.example.ui.components.PauseDialog
import com.example.ui.components.RewardedAdSimulatorModal
import com.example.ui.components.TopHud
import com.example.ui.theme.GameBackground
import com.example.ui.viewmodels.GameplayViewModel

@Composable
fun GameplayScreen(
    viewModel: GameplayViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val activeRewardedAd by AdManager.activeRewardedAd.collectAsState()

    // Handle back button: pause if playing, or navigate back if in dialog
    BackHandler {
        if (!state.isPaused && !state.isGameOver && !state.isLevelCompleted) {
            viewModel.pause()
        } else {
            onNavigateBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("gameplay_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top HUD
            TopHud(
                levelTitle = state.levelTitle,
                difficulty = state.difficulty,
                remainingLives = state.lives,
                maxLives = state.maxLives,
                arrowsRemaining = state.activeArrows.size,
                onBackClicked = { viewModel.pause() },
                onPauseClicked = { viewModel.pause() }
            )

            // Central Arrow Board Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                state.levelDefinition?.let { def ->
                    ArrowBoardCanvas(
                        level = def,
                        activeArrows = state.activeArrows,
                        escapingArrow = state.escapingArrow,
                        escapeProgress = state.escapeProgress,
                        shakingArrowId = state.shakingArrowId,
                        hintArrowId = state.hintArrowId,
                        blockedCollisionPoints = state.blockedCollisionPoints,
                        isZoomMode = state.isZoomMode,
                        onArrowTapped = { arrowId -> viewModel.onArrowTapped(arrowId) }
                    )
                }
            }

            // Bottom Action Controls
            ActionControls(
                hintsRemaining = state.hintsAvailable,
                canUndo = state.canUndo,
                isZoomMode = state.isZoomMode,
                onHintClicked = { viewModel.useHint() },
                onZoomClicked = { viewModel.toggleZoom() },
                onUndoClicked = { viewModel.undo() },
                onResetClicked = { viewModel.restart() }
            )
        }

        // Modals & Dialogs
        if (state.isLevelCompleted) {
            LevelCompletedDialog(
                stars = state.starsEarned,
                moves = state.movesCount,
                timeSeconds = state.elapsedSeconds,
                onNextLevel = { viewModel.nextLevel() },
                onReplay = { viewModel.restart() },
                onMainMenu = onNavigateBack
            )
        }

        if (state.isGameOver) {
            GameOverDialog(
                onWatchAdForLife = { viewModel.watchAdForLife() },
                onRetry = { viewModel.restart() },
                onMainMenu = onNavigateBack
            )
        }

        if (state.isPaused) {
            PauseDialog(
                soundEnabled = viewModel.soundManager.isSoundEnabled,
                musicEnabled = viewModel.soundManager.isMusicEnabled,
                vibrationEnabled = viewModel.hapticManager.isVibrationEnabled,
                onSoundToggled = { viewModel.setSound(it) },
                onMusicToggled = { viewModel.setMusic(it) },
                onVibrationToggled = { viewModel.setVibration(it) },
                onResume = { viewModel.resume() },
                onRestart = { viewModel.restart() },
                onMainMenu = onNavigateBack
            )
        }

        // Rewarded Video Ad Modal
        activeRewardedAd?.let { request ->
            RewardedAdSimulatorModal(
                title = request.title,
                onAdCompleted = { earned ->
                    AdManager.completeRewardedAd(earned)
                }
            )
        }
    }
}
