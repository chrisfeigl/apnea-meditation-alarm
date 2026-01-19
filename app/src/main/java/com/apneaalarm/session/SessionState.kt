package com.apneaalarm.session

sealed class SessionState {
    object Idle : SessionState()

    // Intro bowl phase: 0-54 seconds
    data class IntroBowl(
        val elapsedSeconds: Int,
        val phase: IntroBowlPhase
    ) : SessionState() {
        enum class IntroBowlPhase {
            FADING_IN,   // 0-48s
            HOLDING,     // 48-51s
            FADING_OUT   // 51-54s
        }
    }

    // Pre-hold countdown: 57-60 seconds (3 breath chimes then hold chime)
    data class PreHoldCountdown(
        val countdownSeconds: Int  // 3, 2, 1, 0
    ) : SessionState()

    // Breath hold interval
    data class Holding(
        val cycleIndex: Int,
        val totalCycles: Int,
        val elapsedSeconds: Int,
        val targetSeconds: Int,
        val isCountdown: Boolean = false  // true when in final 3 seconds
    ) : SessionState()

    // Breathing interval
    data class Breathing(
        val cycleIndex: Int,
        val totalCycles: Int,
        val elapsedSeconds: Int,
        val targetSeconds: Int,
        val isCountdown: Boolean = false  // true when in final 3 seconds
    ) : SessionState()

    // Session complete, continuous bowl playing
    object Finishing : SessionState()

    // Session stopped
    object Stopped : SessionState()
}

data class SessionProgress(
    val state: SessionState = SessionState.Idle,
    val currentCycle: Int = 0,
    val totalCycles: Int = 10,
    val totalElapsedSeconds: Int = 0,
    val isActive: Boolean = false
)
