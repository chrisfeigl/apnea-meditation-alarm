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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.apneaalarm.alarm.AlarmScheduler
import com.apneaalarm.audio.AudioPlayer
import com.apneaalarm.data.PreferencesRepository
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
    private var previewAudioPlayer: AudioPlayer? = null

    private val preferencesFlow = MutableStateFlow(UserPreferences())
    private val sessionProgressFlow = MutableStateFlow(SessionProgress())
    private var isPreviewPlaying by mutableStateOf(false)

    private var sessionService: SessionService? = null
    private var serviceBound by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SessionService.LocalBinder
            sessionService = binder.getService()
            serviceBound = true

            lifecycleScope.launch {
                sessionService?.sessionProgress?.collect { progress ->
                    sessionProgressFlow.value = progress
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

        lifecycleScope.launch {
            preferencesRepository.userPreferences.collect { prefs ->
                preferencesFlow.value = prefs
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
                        sessionProgressFlow = sessionProgressFlow,
                        onStartSession = { skipIntro ->
                            startSession(skipIntro)
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
                        onAlarmEnabledChanged = { enabled ->
                            handleAlarmEnabledChanged(enabled)
                        },
                        onAlarmTimeChanged = { hour, minute ->
                            handleAlarmTimeChanged(hour, minute)
                        },
                        onBreathHoldChanged = { seconds ->
                            handleBreathHoldChanged(seconds)
                        },
                        onIntroBowlVolumeChanged = { volume ->
                            handleIntroBowlVolumeChanged(volume)
                        },
                        onBreathChimeVolumeChanged = { volume ->
                            handleBreathChimeVolumeChanged(volume)
                        },
                        onHoldChimeVolumeChanged = { volume ->
                            handleHoldChimeVolumeChanged(volume)
                        },
                        onTrainingModeChanged = { mode ->
                            handleTrainingModeChanged(mode)
                        },
                        onFadeInIntroBowlChanged = { fadeIn ->
                            handleFadeInIntroBowlChanged(fadeIn)
                        },
                        onIntroBowlUriChanged = { uri ->
                            handleIntroBowlUriChanged(uri)
                        },
                        onBreathChimeUriChanged = { uri ->
                            handleBreathChimeUriChanged(uri)
                        },
                        onHoldChimeUriChanged = { uri ->
                            handleHoldChimeUriChanged(uri)
                        },
                        onSnoozeDurationChanged = { minutes ->
                            handleSnoozeDurationChanged(minutes)
                        },
                        onPreviewSound = { uri, soundType ->
                            handlePreviewSound(uri, soundType)
                        },
                        onStopPreview = {
                            handleStopPreview()
                        },
                        isPreviewPlaying = isPreviewPlaying,
                        onCompleteSetup = { mode, breathHoldSeconds ->
                            handleCompleteSetup(mode, breathHoldSeconds)
                        },
                        onUseManualChanged = { useManual ->
                            handleUseManualChanged(useManual)
                        },
                        onManualSettingsChanged = { h, r0, rn, n, p ->
                            handleManualSettingsChanged(h, r0, rn, n, p)
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

    override fun onDestroy() {
        super.onDestroy()
        previewAudioPlayer?.release()
        previewAudioPlayer = null
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

    private fun startSession(skipIntro: Boolean) {
        val intent = Intent(this, SessionService::class.java).apply {
            action = SessionService.ACTION_START_SESSION
            putExtra(SessionService.EXTRA_SKIP_INTRO, skipIntro)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun handleAlarmEnabledChanged(enabled: Boolean) {
        lifecycleScope.launch {
            preferencesRepository.updateAlarmEnabled(enabled)
            val prefs = preferencesRepository.userPreferences.first()
            if (enabled) {
                alarmScheduler.scheduleAlarm(prefs.alarmHour, prefs.alarmMinute)
            } else {
                alarmScheduler.cancelAlarm()
            }
        }
    }

    private fun handleAlarmTimeChanged(hour: Int, minute: Int) {
        lifecycleScope.launch {
            preferencesRepository.updateAlarmTime(hour, minute)
            val prefs = preferencesRepository.userPreferences.first()
            if (prefs.alarmEnabled) {
                alarmScheduler.scheduleAlarm(hour, minute)
            }
        }
    }

    private fun handleBreathHoldChanged(seconds: Int) {
        lifecycleScope.launch {
            preferencesRepository.updateMaxStaticBreathHoldDuration(seconds)
        }
    }

    private fun handleIntroBowlVolumeChanged(volume: Int) {
        lifecycleScope.launch {
            preferencesRepository.updateVolumeMultipliers(introBowl = volume)
        }
    }

    private fun handleBreathChimeVolumeChanged(volume: Int) {
        lifecycleScope.launch {
            preferencesRepository.updateVolumeMultipliers(breathChime = volume)
        }
    }

    private fun handleHoldChimeVolumeChanged(volume: Int) {
        lifecycleScope.launch {
            preferencesRepository.updateVolumeMultipliers(holdChime = volume)
        }
    }

    private fun handleTrainingModeChanged(mode: TrainingMode) {
        lifecycleScope.launch {
            preferencesRepository.updateTrainingMode(mode)
        }
    }

    private fun handleCompleteSetup(mode: TrainingMode, breathHoldSeconds: Int) {
        lifecycleScope.launch {
            preferencesRepository.completeSetup(mode, breathHoldSeconds)
        }
    }

    private fun handleUseManualChanged(useManual: Boolean) {
        lifecycleScope.launch {
            preferencesRepository.updateUseManualIntervalSettings(useManual)
        }
    }

    private fun handleManualSettingsChanged(h: Int?, r0: Int?, rn: Int?, n: Int?, p: Float?) {
        lifecycleScope.launch {
            preferencesRepository.updateManualIntervalSettings(
                breathHoldDuration = h,
                r0Seconds = r0,
                rnSeconds = rn,
                numberOfIntervals = n,
                pFactor = p
            )
        }
    }

    private fun handleSnoozeDurationChanged(minutes: Int) {
        lifecycleScope.launch {
            preferencesRepository.updateSnoozeDuration(minutes)
        }
    }

    private fun handleFadeInIntroBowlChanged(fadeIn: Boolean) {
        lifecycleScope.launch {
            preferencesRepository.updateFadeInIntroBowl(fadeIn)
        }
    }

    private fun handleIntroBowlUriChanged(uri: String?) {
        lifecycleScope.launch {
            if (uri != null) {
                // Take persistent permission for the URI
                val parsedUri = android.net.Uri.parse(uri)
                val permissionTaken = takePersistablePermission(parsedUri)
                if (permissionTaken) {
                    preferencesRepository.updateCustomSoundUri(introBowlUri = uri)
                    android.util.Log.i("MainActivity", "Saved custom intro bowl URI: $uri")
                } else {
                    android.util.Log.e("MainActivity", "Failed to get permission for URI: $uri")
                }
            } else {
                preferencesRepository.updateCustomSoundUri(clearIntroBowl = true)
            }
        }
    }

    private fun handleBreathChimeUriChanged(uri: String?) {
        lifecycleScope.launch {
            if (uri != null) {
                val parsedUri = android.net.Uri.parse(uri)
                val permissionTaken = takePersistablePermission(parsedUri)
                if (permissionTaken) {
                    preferencesRepository.updateCustomSoundUri(breathChimeUri = uri)
                    android.util.Log.i("MainActivity", "Saved custom breath chime URI: $uri")
                } else {
                    android.util.Log.e("MainActivity", "Failed to get permission for URI: $uri")
                }
            } else {
                preferencesRepository.updateCustomSoundUri(clearBreathChime = true)
            }
        }
    }

    private fun handleHoldChimeUriChanged(uri: String?) {
        lifecycleScope.launch {
            if (uri != null) {
                val parsedUri = android.net.Uri.parse(uri)
                val permissionTaken = takePersistablePermission(parsedUri)
                if (permissionTaken) {
                    preferencesRepository.updateCustomSoundUri(holdChimeUri = uri)
                    android.util.Log.i("MainActivity", "Saved custom hold chime URI: $uri")
                } else {
                    android.util.Log.e("MainActivity", "Failed to get permission for URI: $uri")
                }
            } else {
                preferencesRepository.updateCustomSoundUri(clearHoldChime = true)
            }
        }
    }

    private fun handlePreviewSound(uri: String?, soundType: String) {
        // Create or reuse the preview audio player
        if (previewAudioPlayer == null) {
            previewAudioPlayer = AudioPlayer(applicationContext)
        }

        val player = previewAudioPlayer ?: return

        lifecycleScope.launch {
            val prefs = preferencesRepository.userPreferences.first()

            isPreviewPlaying = true

            val onComplete: () -> Unit = {
                isPreviewPlaying = false
            }

            when (soundType) {
                "intro_bowl" -> player.playIntroBowlPreview(
                    prefs.introBowlVolumeMultiplier,
                    uri,
                    onComplete
                )
                "breath_chime" -> player.playBreathChimePreview(
                    prefs.breathChimeVolumeMultiplier,
                    uri,
                    onComplete
                )
                "hold_chime" -> player.playHoldChimePreview(
                    prefs.holdChimeVolumeMultiplier,
                    uri,
                    onComplete
                )
            }
        }
    }

    private fun handleStopPreview() {
        previewAudioPlayer?.stopPreview()
        isPreviewPlaying = false
    }

    private fun takePersistablePermission(uri: android.net.Uri): Boolean {
        return try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            android.util.Log.i("MainActivity", "Successfully took persistable permission for: $uri")
            true
        } catch (e: SecurityException) {
            android.util.Log.e("MainActivity", "SecurityException taking permission: ${e.message}")
            // Still save the URI - it might work with temporary permission
            true
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Exception taking permission: ${e.message}")
            true
        }
    }

    private fun stopSession() {
        sessionService?.stopSession()
    }

    private fun skipIntroAndStartExercises() {
        // Stop current session and restart with skip intro
        sessionService?.stopSession()
        startSession(skipIntro = true)
    }

    private fun snoozeAlarm() {
        lifecycleScope.launch {
            val prefs = preferencesRepository.userPreferences.first()
            // Stop the current session
            sessionService?.stopSession()
            // Schedule a new alarm for snooze duration from now
            alarmScheduler.scheduleSnooze(prefs.snoozeDurationMinutes)
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
