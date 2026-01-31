package com.apneaalarm

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.apneaalarm.alarm.AlarmScheduler
import com.apneaalarm.data.Alarm
import com.apneaalarm.data.PreferencesRepository
import com.apneaalarm.data.SavedSession
import com.apneaalarm.data.SessionSettings
import com.apneaalarm.data.TrainingMode
import com.apneaalarm.data.UserPreferences
import com.apneaalarm.session.SessionProgress
import com.apneaalarm.session.SessionService
import com.apneaalarm.ui.navigation.ApneaNavGraph
import com.apneaalarm.ui.navigation.Screen
import com.apneaalarm.ui.theme.ApneaAlarmTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var alarmScheduler: AlarmScheduler

    private val preferencesFlow = MutableStateFlow(UserPreferences())
    private val alarmsFlow = MutableStateFlow<List<Alarm>>(emptyList())
    private val savedSessionsFlow = MutableStateFlow<List<SavedSession>>(emptyList())
    private val sessionProgressFlow = MutableStateFlow(SessionProgress())
    private val snoozeEnabledFlow = MutableStateFlow(false)
    private val snoozeDurationFlow = MutableStateFlow(5)

    // Cache of alarms for quick access in composable
    private var alarmsCache: List<Alarm> = emptyList()
    // Cache of saved sessions for quick access in composable
    private var savedSessionsCache: List<SavedSession> = emptyList()

    private var sessionService: SessionService? = null
    private var serviceBound by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SessionService.LocalBinder
            sessionService = binder.getService()
            serviceBound = true

            // Update snooze state from service
            sessionService?.let { svc ->
                snoozeEnabledFlow.value = svc.snoozeEnabled
                snoozeDurationFlow.value = svc.snoozeDuration
            }

            lifecycleScope.launch {
                sessionService?.sessionProgress?.collect { progress ->
                    sessionProgressFlow.value = progress
                    // Update snooze state whenever session progress updates
                    // This ensures snooze state is correct for alarm-triggered sessions
                    sessionService?.let { svc ->
                        snoozeEnabledFlow.value = svc.snoozeEnabled
                        snoozeDurationFlow.value = svc.snoozeDuration
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sessionService = null
            serviceBound = false
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesRepository = PreferencesRepository(applicationContext)
        alarmScheduler = AlarmScheduler(applicationContext)

        requestNotificationPermission()

        // Collect user preferences
        lifecycleScope.launch {
            preferencesRepository.userPreferences.collect { prefs ->
                preferencesFlow.value = prefs
            }
        }

        // Collect alarms
        lifecycleScope.launch {
            preferencesRepository.alarmsFlow.collect { alarms ->
                alarmsFlow.value = alarms
                alarmsCache = alarms
                // Reschedule all alarms when list changes
                alarmScheduler.scheduleAllAlarms(alarms)
            }
        }

        // Collect saved sessions
        lifecycleScope.launch {
            preferencesRepository.savedSessionsFlow.collect { sessions ->
                savedSessionsFlow.value = sessions
                savedSessionsCache = sessions
            }
        }

        setContent {
            ApneaAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Navigate to session screen if coming from notification
                    val fromNotification = intent?.getBooleanExtra("from_notification", false) ?: false
                    if (fromNotification && serviceBound) {
                        navController.navigate(Screen.Session.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }

                    ApneaNavGraph(
                        navController = navController,
                        preferencesFlow = preferencesFlow,
                        alarmsFlow = alarmsFlow,
                        savedSessionsFlow = savedSessionsFlow,
                        sessionProgressFlow = sessionProgressFlow,
                        snoozeEnabledFlow = snoozeEnabledFlow,
                        snoozeDurationFlow = snoozeDurationFlow,
                        onStartSessionWithSettings = { settings ->
                            startSessionWithSettings(settings)
                        },
                        onStopSession = {
                            stopSession()
                        },
                        onSkipIntro = {
                            skipIntroAndStartExercises()
                        },
                        onSnooze = {
                            snoozeAlarm()
                        },
                        onPauseSession = {
                            pauseSession()
                        },
                        onResumeSession = {
                            resumeSession()
                        },
                        onSaveAlarm = { alarm ->
                            handleSaveAlarm(alarm)
                        },
                        onDeleteAlarm = { alarmId ->
                            handleDeleteAlarm(alarmId)
                        },
                        onAlarmEnabledChanged = { alarmId, enabled ->
                            handleAlarmEnabledChanged(alarmId, enabled)
                        },
                        onSaveSession = { name, settings ->
                            handleSaveSession(name, settings)
                        },
                        onDeleteSavedSession = { sessionId ->
                            handleDeleteSavedSession(sessionId)
                        },
                        onMaxBreathHoldChanged = { seconds ->
                            handleMaxBreathHoldChanged(seconds)
                        },
                        onCompleteSetup = { mode, breathHoldSeconds ->
                            handleCompleteSetup(mode, breathHoldSeconds)
                        },
                        getAlarmById = { alarmId ->
                            alarmsCache.find { it.id == alarmId }
                        },
                        getSavedSessionById = { sessionId ->
                            savedSessionsCache.find { it.id == sessionId }
                        },
                        onUpdateSavedSession = { savedSession ->
                            handleUpdateSavedSession(savedSession)
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindToSessionService()
    }

    override fun onStop() {
        super.onStop()
        unbindFromSessionService()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun bindToSessionService() {
        Intent(this, SessionService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindFromSessionService() {
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun startSessionWithSettings(settings: SessionSettings) {
        lifecycleScope.launch {
            // Save as last session
            preferencesRepository.updateLastSession(settings)

            val prefs = preferencesRepository.userPreferences.first()
            val globalM = prefs.maxStaticBreathHoldDurationSeconds

            val intent = Intent(this@MainActivity, SessionService::class.java).apply {
                action = SessionService.ACTION_START_SESSION
                putExtra(SessionService.EXTRA_SKIP_INTRO, false)
                putExtra(SessionService.EXTRA_SESSION_SETTINGS_JSON, preferencesRepository.sessionSettingsToJson(settings))
                putExtra(SessionService.EXTRA_GLOBAL_M, globalM)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            // Update snooze state for manual sessions (no snooze)
            snoozeEnabledFlow.value = false
        }
    }

    private fun handleSaveAlarm(alarm: Alarm) {
        lifecycleScope.launch {
            preferencesRepository.saveAlarm(alarm)
            // Scheduling happens automatically via the alarmsFlow collector
        }
    }

    private fun handleDeleteAlarm(alarmId: Long) {
        lifecycleScope.launch {
            alarmScheduler.cancelAlarm(alarmId)
            preferencesRepository.deleteAlarm(alarmId)
        }
    }

    private fun handleAlarmEnabledChanged(alarmId: Long, enabled: Boolean) {
        lifecycleScope.launch {
            preferencesRepository.updateAlarmEnabled(alarmId, enabled)
            // Alarm will be rescheduled via the alarmsFlow collector
        }
    }

    private fun handleSaveSession(name: String, settings: SessionSettings) {
        lifecycleScope.launch {
            val savedSession = SavedSession(
                id = System.currentTimeMillis(),
                name = name,
                sessionSettings = settings
            )
            preferencesRepository.saveSession(savedSession)
        }
    }

    private fun handleDeleteSavedSession(sessionId: Long) {
        lifecycleScope.launch {
            preferencesRepository.deleteSavedSession(sessionId)
        }
    }

    private fun handleUpdateSavedSession(savedSession: SavedSession) {
        lifecycleScope.launch {
            preferencesRepository.updateSavedSession(savedSession)
        }
    }

    private fun handleMaxBreathHoldChanged(seconds: Int) {
        lifecycleScope.launch {
            preferencesRepository.updateMaxStaticBreathHoldDuration(seconds)
        }
    }

    private fun handleCompleteSetup(mode: TrainingMode, breathHoldSeconds: Int) {
        lifecycleScope.launch {
            preferencesRepository.completeSetup(mode, breathHoldSeconds)
        }
    }

    private fun stopSession() {
        sessionService?.stopSession()
    }

    private fun pauseSession() {
        sessionService?.pauseSession()
    }

    private fun resumeSession() {
        sessionService?.resumeSession()
    }

    private fun skipIntroAndStartExercises() {
        // Stop current session and restart with skip intro
        // Get current settings from service if available
        sessionService?.stopSession()

        lifecycleScope.launch {
            val prefs = preferencesRepository.userPreferences.first()
            val settings = prefs.lastSessionSettings ?: SessionSettings()
            val globalM = prefs.maxStaticBreathHoldDurationSeconds

            val intent = Intent(this@MainActivity, SessionService::class.java).apply {
                action = SessionService.ACTION_START_SESSION
                putExtra(SessionService.EXTRA_SKIP_INTRO, true)
                putExtra(SessionService.EXTRA_SESSION_SETTINGS_JSON, preferencesRepository.sessionSettingsToJson(settings))
                putExtra(SessionService.EXTRA_GLOBAL_M, globalM)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun snoozeAlarm() {
        lifecycleScope.launch {
            val service = sessionService ?: return@launch

            // Stop the current session
            service.stopSession()

            // Schedule snooze using the alarm's snooze settings
            if (service.snoozeEnabled) {
                alarmScheduler.scheduleSnooze(
                    minutes = service.snoozeDuration,
                    alarmId = service.alarmId,
                    snoozeDurationMinutes = service.snoozeDuration,
                    snoozeEnabled = service.snoozeEnabled
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
