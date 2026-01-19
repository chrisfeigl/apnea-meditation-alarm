package com.apneaalarm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.apneaalarm.data.TrainingMode
import com.apneaalarm.data.UserPreferences
import com.apneaalarm.session.SessionProgress
import com.apneaalarm.ui.screens.AudioFilesScreen
import com.apneaalarm.ui.screens.HomeScreen
import com.apneaalarm.ui.screens.SessionScreen
import com.apneaalarm.ui.screens.SettingsScreen
import com.apneaalarm.ui.screens.WelcomeScreen
import kotlinx.coroutines.flow.StateFlow

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Home : Screen("home")
    object Session : Screen("session")
    object Settings : Screen("settings")
    object AudioFiles : Screen("audio_files")
}

@Composable
fun ApneaNavGraph(
    navController: NavHostController,
    preferencesFlow: StateFlow<UserPreferences>,
    sessionProgressFlow: StateFlow<SessionProgress>,
    onStartSession: (skipIntro: Boolean) -> Unit,
    onStopSession: () -> Unit,
    onSkipIntro: () -> Unit,
    onSnooze: () -> Unit,
    onAlarmEnabledChanged: (Boolean) -> Unit,
    onAlarmTimeChanged: (Int, Int) -> Unit,
    onBreathHoldChanged: (Int) -> Unit,
    onIntroBowlVolumeChanged: (Int) -> Unit,
    onBreathChimeVolumeChanged: (Int) -> Unit,
    onHoldChimeVolumeChanged: (Int) -> Unit,
    onTrainingModeChanged: (TrainingMode) -> Unit,
    onFadeInIntroBowlChanged: (Boolean) -> Unit,
    onIntroBowlUriChanged: (String?) -> Unit,
    onBreathChimeUriChanged: (String?) -> Unit,
    onHoldChimeUriChanged: (String?) -> Unit,
    onSnoozeDurationChanged: (Int) -> Unit,
    onPreviewSound: (uri: String?, soundType: String) -> Unit,
    onStopPreview: () -> Unit,
    isPreviewPlaying: Boolean,
    onCompleteSetup: (TrainingMode, Int) -> Unit
) {
    val preferences by preferencesFlow.collectAsState()
    val sessionProgress by sessionProgressFlow.collectAsState()

    val startDestination = if (preferences.isFirstTimeSetupComplete) {
        Screen.Home.route
    } else {
        Screen.Welcome.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onComplete = { mode, breathHoldSeconds ->
                    onCompleteSetup(mode, breathHoldSeconds)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                preferences = preferences,
                isSessionActive = sessionProgress.isActive,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToSession = {
                    navController.navigate(Screen.Session.route)
                },
                onStartSession = { skipIntro ->
                    onStartSession(skipIntro)
                    navController.navigate(Screen.Session.route)
                }
            )
        }

        composable(Screen.Session.route) {
            SessionScreen(
                progress = sessionProgress,
                onStop = {
                    onStopSession()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Session.route) { inclusive = true }
                    }
                },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Session.route) { inclusive = true }
                    }
                },
                onSkipIntro = onSkipIntro,
                onSnooze = {
                    onSnooze()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Session.route) { inclusive = true }
                    }
                },
                snoozeDurationMinutes = preferences.snoozeDurationMinutes
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                preferences = preferences,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAudioFiles = {
                    navController.navigate(Screen.AudioFiles.route)
                },
                onAlarmEnabledChanged = onAlarmEnabledChanged,
                onAlarmTimeChanged = onAlarmTimeChanged,
                onBreathHoldChanged = onBreathHoldChanged,
                onIntroBowlVolumeChanged = onIntroBowlVolumeChanged,
                onBreathChimeVolumeChanged = onBreathChimeVolumeChanged,
                onHoldChimeVolumeChanged = onHoldChimeVolumeChanged,
                onTrainingModeChanged = onTrainingModeChanged,
                onSnoozeDurationChanged = onSnoozeDurationChanged,
                onFadeInIntroBowlChanged = onFadeInIntroBowlChanged
            )
        }

        composable(Screen.AudioFiles.route) {
            AudioFilesScreen(
                preferences = preferences,
                onNavigateBack = { navController.popBackStack() },
                onIntroBowlUriChanged = onIntroBowlUriChanged,
                onBreathChimeUriChanged = onBreathChimeUriChanged,
                onHoldChimeUriChanged = onHoldChimeUriChanged,
                onPreviewSound = onPreviewSound,
                onStopPreview = onStopPreview,
                isPreviewPlaying = isPreviewPlaying
            )
        }
    }
}
