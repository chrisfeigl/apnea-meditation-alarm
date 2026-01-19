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

        // Play breath chime to signal end of hold / start of breathing
        audioPlayer.playBreathChime(
            preferences.breathChimeVolumeMultiplier,
            preferences.customBreathChimeUri
        )
    }

    private suspend fun runBreathingInterval(cycleIndex: Int) {
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

        // Play hold chime to signal end of breathing / start of next hold
        audioPlayer.playHoldChime(
            preferences.holdChimeVolumeMultiplier,
            preferences.customHoldChimeUri
        )
    }

    private fun startFinishingBowl() {
        _progress.value = SessionProgress(
            state = SessionState.Finishing,
            currentCycle = preferences.numberOfIntervals,
            totalCycles = preferences.numberOfIntervals,
            totalElapsedSeconds = totalElapsedSeconds,
            isActive = true
        )

        audioPlayer.startContinuousBowl(
            preferences.introBowlVolumeMultiplier,
            preferences.customIntroBowlUri
        )
    }
}
