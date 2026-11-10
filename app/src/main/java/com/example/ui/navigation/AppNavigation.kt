package com.example.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.GameRepository
import com.example.data.UserPreferencesRepository
import com.example.ui.screens.DailyChallengeScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TrophyScreen
import com.example.ui.viewmodels.GameplayViewModel
import com.example.ui.viewmodels.HomeViewModel
import com.example.ui.viewmodels.SettingsViewModel
import com.example.utils.HapticManager
import com.example.utils.SoundEffectManager

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object LevelSelect : Screen("level_select")
    object Gameplay : Screen("gameplay/{levelNumber}/{isDaily}") {
        fun createRoute(levelNumber: Int, isDaily: Boolean = false) = "gameplay/$levelNumber/$isDaily"
    }
    object DailyChallenge : Screen("daily_challenge")
    object Trophies : Screen("trophies")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext

    // Create shared singletons
    val database = remember { AppDatabase.getInstance(context) }
    val preferences = remember { UserPreferencesRepository(context) }
    val repository = remember { GameRepository(database, preferences) }
    val soundManager = remember { SoundEffectManager() }
    val hapticManager = remember { HapticManager(context) }

    // Shared ViewModels
    val homeViewModel = remember { HomeViewModel(repository) }
    val settingsViewModel = remember { SettingsViewModel(repository) }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onPlayClicked = { levelNum ->
                    navController.navigate(Screen.Gameplay.createRoute(levelNum, false))
                },
                onDailyChallengeClicked = {
                    navController.navigate(Screen.DailyChallenge.route)
                },
                onLevelSelectClicked = {
                    navController.navigate(Screen.LevelSelect.route)
                },
                onTrophiesClicked = {
                    navController.navigate(Screen.Trophies.route)
                },
                onSettingsClicked = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.LevelSelect.route) {
            LevelSelectScreen(
                viewModel = homeViewModel,
                onLevelSelected = { levelNum ->
                    navController.navigate(Screen.Gameplay.createRoute(levelNum, false))
                },
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Gameplay.route,
            arguments = listOf(
                navArgument("levelNumber") { type = NavType.IntType; defaultValue = 1 },
                navArgument("isDaily") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val levelNum = backStackEntry.arguments?.getInt("levelNumber") ?: 1
            val isDaily = backStackEntry.arguments?.getBoolean("isDaily") ?: false

            val gameplayViewModel = remember(levelNum, isDaily) {
                GameplayViewModel(repository, soundManager, hapticManager).apply {
                    if (isDaily) {
                        startDailyChallenge()
                    } else {
                        startLevel(levelNum)
                    }
                }
            }

            GameplayScreen(
                viewModel = gameplayViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.DailyChallenge.route) {
            DailyChallengeScreen(
                viewModel = homeViewModel,
                onStartDailyChallenge = {
                    navController.navigate(Screen.Gameplay.createRoute(101, true))
                },
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Trophies.route) {
            TrophyScreen(
                viewModel = homeViewModel,
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClicked = {
                    navController.popBackStack()
                }
            )
        }
    }
}
