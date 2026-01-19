package com.apneaalarm.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.apneaalarm.MainActivity
import com.apneaalarm.R
import com.apneaalarm.data.PreferencesRepository
import com.apneaalarm.data.SessionSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SessionService : Service() {

    private val binder = LocalBinder()
    private var breathingSession: BreathingSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var stopReceiver: BroadcastReceiver? = null

    private val _sessionProgress = MutableStateFlow(SessionProgress())
    val sessionProgress: StateFlow<SessionProgress> = _sessionProgress.asStateFlow()

    // Snooze info for alarm-triggered sessions
    private var currentAlarmId: Long = -1L
    private var currentSnoozeDuration: Int = 5
    private var currentSnoozeEnabled: Boolean = true

    // Expose snooze state
    val snoozeEnabled: Boolean get() = currentSnoozeEnabled
    val snoozeDuration: Int get() = currentSnoozeDuration
    val alarmId: Long get() = currentAlarmId

    companion object {
        const val ACTION_START_SESSION = "com.apneaalarm.START_SESSION"
        const val ACTION_STOP_SESSION = "com.apneaalarm.STOP_SESSION"
        const val ACTION_STOP_BROADCAST = "com.apneaalarm.STOP_BROADCAST"

        // Intent extras
        const val EXTRA_SKIP_INTRO = "skip_intro"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_SESSION_SETTINGS_JSON = "session_settings_json"
        const val EXTRA_SNOOZE_DURATION = "snooze_duration"
        const val EXTRA_SNOOZE_ENABLED = "snooze_enabled"
        const val EXTRA_GLOBAL_M = "global_m"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "apnea_alarm_session"
        private const val CHANNEL_ID_FINISH = "apnea_alarm_finish"
    }

    private var pendingSkipIntro = false
    private var pendingAlarmId: Long = -1L
    private var pendingSessionSettingsJson: String? = null
    private var pendingGlobalM: Int = 60
    private var pendingSnoozeDuration: Int = 5
    private var pendingSnoozeEnabled: Boolean = true

    inner class LocalBinder : Binder() {
        fun getService(): SessionService = this@SessionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        acquireWakeLock()
        registerStopReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> {
                pendingSkipIntro = intent.getBooleanExtra(EXTRA_SKIP_INTRO, false)
                pendingAlarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                pendingSessionSettingsJson = intent.getStringExtra(EXTRA_SESSION_SETTINGS_JSON)
                pendingGlobalM = intent.getIntExtra(EXTRA_GLOBAL_M, 60)
                pendingSnoozeDuration = intent.getIntExtra(EXTRA_SNOOZE_DURATION, 5)
                pendingSnoozeEnabled = intent.getBooleanExtra(EXTRA_SNOOZE_ENABLED, true)
                startSession()
            }
            ACTION_STOP_SESSION -> stopSession()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterStopReceiver()
        stopSession()
        releaseWakeLock()
        serviceScope.cancel()
    }

    private fun registerStopReceiver() {
        stopReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_STOP_BROADCAST) {
                    stopSession()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP_BROADCAST), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopReceiver, IntentFilter(ACTION_STOP_BROADCAST))
        }
    }

    private fun unregisterStopReceiver() {
        stopReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // Receiver might not be registered
            }
        }
        stopReceiver = null
    }

    private fun startSession() {
        startForeground(NOTIFICATION_ID, createNotification("Apnea session starting...", false))

        serviceScope.launch {
            val repository = PreferencesRepository(applicationContext)
            val prefs = repository.userPreferences.first()

            // Determine session settings and global M
            val sessionSettings: SessionSettings
            val globalM: Int

            if (pendingSessionSettingsJson != null) {
                // Manual session with settings passed in intent
                sessionSettings = repository.sessionSettingsFromJson(pendingSessionSettingsJson!!)
                    ?: SessionSettings()
                globalM = if (pendingGlobalM > 0) pendingGlobalM else prefs.maxStaticBreathHoldDurationSeconds
                // Manual sessions don't have snooze by default
                currentSnoozeEnabled = false
                currentSnoozeDuration = 5
                currentAlarmId = -1L
            } else if (pendingAlarmId > 0) {
                // Alarm-triggered session - load settings from alarm
                val alarm = repository.getAlarmById(pendingAlarmId)
                if (alarm != null) {
                    sessionSettings = alarm.sessionSettings
                    currentSnoozeEnabled = pendingSnoozeEnabled
                    currentSnoozeDuration = pendingSnoozeDuration
                    currentAlarmId = pendingAlarmId
                } else {
                    // Alarm not found, use defaults
                    sessionSettings = SessionSettings()
                    currentSnoozeEnabled = pendingSnoozeEnabled
                    currentSnoozeDuration = pendingSnoozeDuration
                    currentAlarmId = pendingAlarmId
                }
                globalM = prefs.maxStaticBreathHoldDurationSeconds
            } else {
                // Fallback: use last session settings or defaults
                sessionSettings = prefs.lastSessionSettings ?: SessionSettings()
                globalM = prefs.maxStaticBreathHoldDurationSeconds
                currentSnoozeEnabled = false
                currentSnoozeDuration = 5
                currentAlarmId = -1L
            }

            breathingSession = BreathingSession(
                context = applicationContext,
                sessionSettings = sessionSettings,
                globalM = globalM,
                skipIntro = pendingSkipIntro
            )

            breathingSession?.let { session ->
                launch {
                    session.progress.collect { progress ->
                        _sessionProgress.value = progress
                        updateNotification(progress)

                        if (progress.state is SessionState.Stopped) {
                            stopSelf()
                        }
                    }
                }

                session.start()
            }
        }
    }

    fun stopSession() {
        breathingSession?.stop()
        breathingSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sessionChannel = NotificationChannel(
                CHANNEL_ID,
                "Apnea Alarm Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress during breathing session"
                setShowBadge(false)
            }

            val finishChannel = NotificationChannel(
                CHANNEL_ID_FINISH,
                "Session Complete",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alert when breathing session is complete"
                setShowBadge(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(sessionChannel)
            notificationManager.createNotificationChannel(finishChannel)
        }
    }

    private fun createNotification(contentText: String, isFinishing: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
            putExtra("show_session", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use service intent directly for stop action
        val stopIntent = Intent(this, SessionService::class.java).apply {
            action = ACTION_STOP_SESSION
        }

        val stopPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val channelId = if (isFinishing) CHANNEL_ID_FINISH else CHANNEL_ID

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(if (isFinishing) "Session Complete!" else "Apnea Alarm")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Always add a prominent stop action
        val stopAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stop,
            if (isFinishing) "STOP" else "STOP",
            stopPendingIntent
        ).build()
        builder.addAction(stopAction)

        if (isFinishing) {
            builder.setFullScreenIntent(pendingIntent, true)
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        } else {
            builder.setSilent(true)
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
        }

        return builder.build()
    }

    private fun updateNotification(progress: SessionProgress) {
        val isFinishing = progress.state is SessionState.Finishing

        val text = when (val state = progress.state) {
            is SessionState.IntroBowl -> {
                val phaseText = when (state.phase) {
                    SessionState.IntroBowl.IntroBowlPhase.FADING_IN -> "Waking up"
                    SessionState.IntroBowl.IntroBowlPhase.HOLDING -> "Bowl at max"
                    SessionState.IntroBowl.IntroBowlPhase.FADING_OUT -> "Bowl fading"
                }
                "$phaseText... ${state.elapsedSeconds}s"
            }
            is SessionState.PreHoldCountdown -> "Get ready... ${state.countdownSeconds}"
            is SessionState.Holding -> {
                val remaining = state.targetSeconds - state.elapsedSeconds
                "HOLD - Cycle ${state.cycleIndex + 1}/${state.totalCycles} - ${remaining}s"
            }
            is SessionState.Breathing -> {
                val remaining = state.targetSeconds - state.elapsedSeconds
                "BREATHE - Cycle ${state.cycleIndex + 1}/${state.totalCycles} - ${remaining}s"
            }
            is SessionState.Finishing -> "Tap to open app and press STOP"
            is SessionState.Stopped -> "Session ended"
            is SessionState.Idle -> "Starting..."
        }

        val notification = createNotification(text, isFinishing)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ApneaAlarm::SessionWakeLock"
        ).apply {
            acquire(60 * 60 * 1000L) // 1 hour max
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}
