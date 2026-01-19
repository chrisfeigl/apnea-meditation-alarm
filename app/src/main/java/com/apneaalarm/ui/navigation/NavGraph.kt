package com.apneaalarm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.apneaalarm.data.Alarm
import com.apneaalarm.data.SavedSession
import com.apneaalarm.data.SessionSettings
import com.apneaalarm.data.TrainingMode
import com.apneaalarm.data.UserPreferences
import com.apneaalarm.session.SessionProgress
import com.apneaalarm.ui.screens.AlarmEditScreen
import com.apneaalarm.ui.screens.EditSavedSessionScreen
import com.apneaalarm.ui.screens.HomeScreen
import com.apneaalarm.ui.screens.NewSessionScreen
import com.apneaalarm.ui.screens.SavedSessionsScreen
import com.apneaalarm.ui.screens.SessionScreen
import com.apneaalarm.ui.screens.SettingsScreen
import com.apneaalarm.ui.screens.WelcomeScreen
import kotlinx.coroutines.flow.StateFlow

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Home : Screen("home")
    object Session : Screen("session")
    object Settings : Screen("settings")
    object NewSession : Screen("new_session")
    object SavedSessions : Screen("saved_sessions")
    object AlarmEdit : Screen("alarm_edit/{alarmId}") {
        fun createRoute(alarmId: Long?) = "alarm_edit/${alarmId ?: -1}"
    }
    object EditSavedSession : Screen("edit_saved_session/{sessionId}") {
        fun createRoute(sessionId: Long) = "edit_saved_session/$sessionId"
    }
}

@Composable
fun ApneaNavGraph(
    navController: NavHostController,
    preferencesFlow: StateFlow<UserPreferences>,
    alarmsFlow: StateFlow<List<Alarm>>,
    savedSessionsFlow: StateFlow<List<SavedSession>>,
    sessionProgressFlow: StateFlow<SessionProgress>,
    snoozeEnabledFlow: StateFlow<Boolean>,
    snoozeDurationFlow: StateFlow<Int>,
    // Session control
    onStartSessionWithSettings: (SessionSettings) -> Unit,
    onStopSession: () -> Unit,
    onSkipIntro: () -> Unit,
    onSnooze: () -> Unit,
    // Alarm management
    onSaveAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
    onAlarmEnabledChanged: (Long, Boolean) -> Unit,
    // Session management
    onSaveSession: (String, SessionSettings) -> Unit,
    onDeleteSavedSession: (Long) -> Unit,
    // Global settings
    onMaxBreathHoldChanged: (Int) -> Unit,
    // Setup
    onCompleteSetup: (TrainingMode, Int) -> Unit,
    // Get alarm by ID (for edit screen)
    getAlarmById: (Long) -> Alarm?,
    // Get saved session by ID (for edit screen)
    getSavedSessionById: (Long) -> SavedSession?,
    // Update saved session
    onUpdateSavedSession: (SavedSession) -> Unit
) {
    val preferences by preferencesFlow.collectAsState()
    val alarms by alarmsFlow.collectAsState()
    val savedSessions by savedSessionsFlow.collectAsState()
    val sessionProgress by sessionProgressFlow.collectAsState()
    val snoozeEnabled by snoozeEnabledFlow.collectAsState()
    val snoozeDuration by snoozeDurationFlow.collectAsState()

    val globalM = preferences.maxStaticBreathHoldDurationSeconds

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
                alarms = alarms,
                isSessionActive = sessionProgress.isActive,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToSession = {
                    navController.navigate(Screen.Session.route)
                },
                onNavigateToNewSession = {
                    navController.navigate(Screen.NewSession.route)
                },
                onNavigateToSavedSessions = {
                    navController.navigate(Screen.SavedSessions.route)
                },
                onNavigateToAlarmEdit = { alarmId ->
                    navController.navigate(Screen.AlarmEdit.createRoute(alarmId))
                },
                onRepeatLastSession = {
                    preferences.lastSessionSettings?.let { settings ->
                        onStartSessionWithSettings(settings)
                        navController.navigate(Screen.Session.route)
                    }
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
                snoozeDurationMinutes = snoozeDuration,
                snoozeEnabled = snoozeEnabled
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                preferences = preferences,
                alarms = alarms,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAlarmEdit = { alarmId ->
                    navController.navigate(Screen.AlarmEdit.createRoute(alarmId))
                },
                onAlarmEnabledChanged = onAlarmEnabledChanged,
                onMaxBreathHoldChanged = onMaxBreathHoldChanged
            )
        }

        composable(Screen.NewSession.route) {
            // Get initial settings from last session or defaults
            val initialSettings = preferences.lastSessionSettings ?: SessionSettings()

            NewSessionScreen(
                initialSettings = initialSettings,
                globalM = globalM,
                onNavigateBack = { navController.popBackStack() },
                onStartSession = { settings ->
                    onStartSessionWithSettings(settings)
                    navController.navigate(Screen.Session.route) {
                        popUpTo(Screen.NewSession.route) { inclusive = true }
                    }
                },
                onSaveSession = { name, settings ->
                    onSaveSession(name, settings)
                }
            )
        }

        composable(Screen.SavedSessions.route) {
            SavedSessionsScreen(
                savedSessions = savedSessions,
                globalM = globalM,
                onNavigateBack = { navController.popBackStack() },
                onStartSession = { savedSession ->
                    onStartSessionWithSettings(savedSession.sessionSettings)
                    navController.navigate(Screen.Session.route) {
                        popUpTo(Screen.SavedSessions.route) { inclusive = true }
                    }
                },
                onEditSession = { savedSession ->
                    // Navigate to edit screen for this saved session
                    navController.navigate(Screen.EditSavedSession.createRoute(savedSession.id))
                },
                onDeleteSession = onDeleteSavedSession
            )
        }

        composable(
            route = Screen.AlarmEdit.route,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L
            val isNewAlarm = alarmId == -1L
            val alarm = if (isNewAlarm) null else getAlarmById(alarmId)

            AlarmEditScreen(
                alarm = alarm,
                globalM = globalM,
                isNewAlarm = isNewAlarm,
                onNavigateBack = { navController.popBackStack() },
                onSaveAlarm = { savedAlarm ->
                    onSaveAlarm(savedAlarm)
                    navController.popBackStack()
                },
                onDeleteAlarm = { id ->
                    onDeleteAlarm(id)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditSavedSession.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: -1L
            val savedSession = getSavedSessionById(sessionId)

            if (savedSession != null) {
                EditSavedSessionScreen(
                    savedSession = savedSession,
                    globalM = globalM,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveSession = { updatedSession ->
                        onUpdateSavedSession(updatedSession)
                        navController.popBackStack()
                    },
                    onDeleteSession = { id ->
                        onDeleteSavedSession(id)
                        navController.popBackStack()
                    },
                    onStartSession = { settings ->
                        onStartSessionWithSettings(settings)
                        navController.navigate(Screen.Session.route) {
                            popUpTo(Screen.EditSavedSession.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
