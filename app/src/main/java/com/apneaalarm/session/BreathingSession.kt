package com.apneaalarm.session

import android.content.Context
import com.apneaalarm.audio.AudioPlayer
import com.apneaalarm.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BreathingSession(
    private val context: Context,
    private val preferences: UserPreferences,
    private val skipIntro: Boolean = false
) {
    private val audioPlayer = AudioPlayer(context)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var sessionJob: Job? = null
    private var finishingJob: Job? = null

    private val _progress = MutableStateFlow(SessionProgress())
    val progress: StateFlow<SessionProgress> = _progress.asStateFlow()

    private var totalElapsedSeconds = 0

    fun start() {
        if (sessionJob?.isActive == true) return

        sessionJob = scope.launch {
            runSession()
        }
    }

    fun stop() {
        sessionJob?.cancel()
        sessionJob = null
        finishingJob?.cancel()
        finishingJob = null
        audioPlayer.stopContinuousBowl()
        audioPlayer.release()
        _progress.value = SessionProgress(state = SessionState.Stopped, isActive = false)
    }

    private suspend fun runSession() {
        _progress.value = SessionProgress(
            state = SessionState.Idle,
            totalCycles = preferences.numberOfIntervals,
            isActive = true
        )
        totalElapsedSeconds = 0

        if (!skipIntro) {
            // Phase 1: Intro Bowl (0:00 - 0:54)
            playIntroBowl()
            if (!scope.isActive) return

            // Gap after bowl ends at 0:54, hold starts at 0:57
            delay(3000) // 54-57: silence
            totalElapsedSeconds = 57

            // Phase 2: Pre-hold countdown display (just visual, single chime)
            playPreHoldCountdown()
            if (!scope.isActive) return
        } else {
            // Skip intro - go straight to first hold
            totalElapsedSeconds = 60
            // Play hold chime to signal start
            audioPlayer.playHoldChime(
                preferences.holdChimeVolumeMultiplier,
                preferences.customHoldChimeUri
            )
        }

        // Phase 3: Breathing cycles
        for (cycleIndex in 0 until preferences.numberOfIntervals) {
            if (!scope.isActive) return

            // Hold interval
            runHoldInterval(cycleIndex)
            if (!scope.isActive) return

            // Breathing interval (except after last cycle)
            if (cycleIndex < preferences.numberOfIntervals - 1) {
                runBreathingInterval(cycleIndex)
                if (!scope.isActive) return
            }
        }

        // Phase 4: Continuous bowl until stopped
        startFinishingBowl()
    }

    private suspend fun playIntroBowl() {
        audioPlayer.playIntroBowl(
            volumeMultiplier = preferences.introBowlVolumeMultiplier,
            customSoundUri = preferences.customIntroBowlUri,
            fadeIn = preferences.fadeInIntroBowl
        ) { elapsedSeconds ->
            totalElapsedSeconds = elapsedSeconds

            val phase = when {
                elapsedSeconds < 48 -> SessionState.IntroBowl.IntroBowlPhase.FADING_IN
                elapsedSeconds < 51 -> SessionState.IntroBowl.IntroBowlPhase.HOLDING
                else -> SessionState.IntroBowl.IntroBowlPhase.FADING_OUT
            }

            _progress.value = SessionProgress(
                state = SessionState.IntroBowl(elapsedSeconds, phase),
                totalCycles = preferences.numberOfIntervals,
                totalElapsedSeconds = totalElapsedSeconds,
                isActive = true
            )
        }
    }

    private suspend fun playPreHoldCountdown() {
        // Visual countdown 3-2-1-0 with single hold chime at the end
        for (countdown in 3 downTo 1) {
            _progress.value = _progress.value.copy(
                state = SessionState.PreHoldCountdown(countdown),
                totalElapsedSeconds = 57 + (3 - countdown)
            )
            delay(1000)
        }

        // At 0, play the hold chime to signal start of first hold
        _progress.value = _progress.value.copy(
            state = SessionState.PreHoldCountdown(0),
            totalElapsedSeconds = 60
        )
        audioPlayer.playHoldChime(
            preferences.holdChimeVolumeMultiplier,
            preferences.customHoldChimeUri
        )

        totalElapsedSeconds = 60
    }

    private suspend fun runHoldInterval(cycleIndex: Int) {
        // Play hold chime at start of hold interval (non-blocking)
        // Skip for first cycle (cycleIndex 0) since pre-hold countdown already played it
        if (cycleIndex > 0) {
            scope.launch {
                audioPlayer.playHoldChime(
                    preferences.holdChimeVolumeMultiplier,
                    preferences.customHoldChimeUri
                )
            }
        }

        val holdDuration = preferences.breathHoldDurationSeconds

        for (elapsed in 0 until holdDuration) {
            if (!scope.isActive) return

            val remaining = holdDuration - elapsed
            val isCountdown = remaining <= 3

            _progress.value = SessionProgress(
                state = SessionState.Holding(
                    cycleIndex = cycleIndex,
                    totalCycles = preferences.numberOfIntervals,
                    elapsedSeconds = elapsed,
                    targetSeconds = holdDuration,
                    isCountdown = isCountdown
                ),
                currentCycle = cycleIndex,
                totalCycles = preferences.numberOfIntervals,
                totalElapsedSeconds = totalElapsedSeconds + elapsed,
                isActive = true
            )

            delay(1000)
        }

        totalElapsedSeconds += holdDuration
    }

    private suspend fun runBreathingInterval(cycleIndex: Int) {
        // Play breath chime at start of breathing interval (non-blocking)
        scope.launch {
            audioPlayer.playBreathChime(
                preferences.breathChimeVolumeMultiplier,
                preferences.customBreathChimeUri
            )
        }

        val breathingDuration = preferences.breathingIntervalDuration(cycleIndex)

        for (elapsed in 0 until breathingDuration) {
            if (!scope.isActive) return

            val remaining = breathingDuration - elapsed
            val isCountdown = remaining <= 3

            _progress.value = SessionProgress(
                state = SessionState.Breathing(
                    cycleIndex = cycleIndex,
                    totalCycles = preferences.numberOfIntervals,
                    elapsedSeconds = elapsed,
                    targetSeconds = breathingDuration,
                    isCountdown = isCountdown
                ),
                currentCycle = cycleIndex,
                totalCycles = preferences.numberOfIntervals,
                totalElapsedSeconds = totalElapsedSeconds + elapsed,
                isActive = true
            )

            delay(1000)
        }

        totalElapsedSeconds += breathingDuration
    }

    private fun startFinishingBowl() {
        _progress.value = SessionProgress(
            state = SessionState.Finishing(elapsedSeconds = 0, isChimePhase = false),
            currentCycle = preferences.numberOfIntervals,
            totalCycles = preferences.numberOfIntervals,
            totalElapsedSeconds = totalElapsedSeconds,
            isActive = true
        )

        // Play bowl at max volume (multiplier 10)
        audioPlayer.startContinuousBowl(
            10,  // Max volume
            preferences.customIntroBowlUri
        )

        // Start a timer for 3 minutes, then switch to chimes
        finishingJob = scope.launch {
            val threeMinutes = 180  // 3 minutes in seconds
            for (elapsed in 0 until threeMinutes) {
                if (!isActive) return@launch
                delay(1000)
                _progress.value = _progress.value.copy(
                    state = SessionState.Finishing(elapsedSeconds = elapsed + 1, isChimePhase = false),
                    totalElapsedSeconds = totalElapsedSeconds + elapsed + 1
                )
            }

            // After 3 minutes, switch to repeated hold chimes at max volume
            if (!isActive) return@launch
            audioPlayer.startContinuousHoldChime(preferences.customHoldChimeUri)

            // Continue tracking time in chime phase
            var chimeElapsed = 0
            while (isActive) {
                delay(1000)
                chimeElapsed++
                _progress.value = _progress.value.copy(
                    state = SessionState.Finishing(elapsedSeconds = threeMinutes + chimeElapsed, isChimePhase = true),
                    totalElapsedSeconds = totalElapsedSeconds + threeMinutes + chimeElapsed
                )
            }
        }
    }
}
