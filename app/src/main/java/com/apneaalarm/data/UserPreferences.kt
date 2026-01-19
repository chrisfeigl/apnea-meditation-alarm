package com.apneaalarm.data

enum class TrainingMode {
    RELAXATION,  // Calm, meditation-focused
    INTENSE      // Performance, CO₂ tolerance training
}

data class UserPreferences(
    // Alarm settings
    val alarmHour: Int = 7,
    val alarmMinute: Int = 0,
    val alarmEnabled: Boolean = false,
    val isFirstTimeSetupComplete: Boolean = false,

    // Training mode
    val trainingMode: TrainingMode = TrainingMode.RELAXATION,

    // Breath hold settings - M (max static breath hold recorded by user)
    val maxStaticBreathHoldDurationSeconds: Int = 60,

    // Volume multipliers (1-10 scale, default 10 = 100% of system alarm volume)
    val introBowlVolumeMultiplier: Int = 10,
    val breathChimeVolumeMultiplier: Int = 10,
    val holdChimeVolumeMultiplier: Int = 10,

    // Custom sound file URIs (null = use default bundled sounds)
    val customIntroBowlUri: String? = null,
    val customBreathChimeUri: String? = null,
    val customHoldChimeUri: String? = null,

    // Intro bowl fade-in option (true = fade in gradually, false = play at full volume immediately)
    val fadeInIntroBowl: Boolean = true,

    // Snooze duration in minutes
    val snoozeDurationMinutes: Int = 5
) {
    // Mode-specific parameters
    // Relaxation: H = 0.60*M, R0 = 1.25*H, Rn = 0.25*H, N = 6, p = 1.4
    // Intense:    H = 0.90*M, R0 = 0.50*H, Rn = 0.12*H, N = 8, p = 0.75

    private val holdMultiplier: Double
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 0.60
            TrainingMode.INTENSE -> 0.90
        }

    private val r0Multiplier: Double
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 1.25
            TrainingMode.INTENSE -> 0.50
        }

    private val rnMultiplier: Double
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 0.25
            TrainingMode.INTENSE -> 0.12
        }

    val numberOfIntervals: Int
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 6
            TrainingMode.INTENSE -> 8
        }

    private val pFactor: Double
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 1.4
            TrainingMode.INTENSE -> 0.75
        }

    // H = breath hold duration based on mode
    val breathHoldDurationSeconds: Int
        get() = (maxStaticBreathHoldDurationSeconds * holdMultiplier).toInt()

    // R0 = max breathing interval
    private val breathingIntervalDurationMaxSeconds: Int
        get() = (breathHoldDurationSeconds * r0Multiplier).toInt()

    // Rn = min breathing interval
    private val breathingIntervalDurationMinSeconds: Int
        get() = (breathHoldDurationSeconds * rnMultiplier).toInt().coerceAtLeast(3)

    // Calculate breathing interval duration for a given interval index
    // Formula: R0 + (Rn - R0) * (i / (N-1))^p
    fun breathingIntervalDuration(intervalIndex: Int): Int {
        if (numberOfIntervals <= 1) return breathingIntervalDurationMaxSeconds

        val i = intervalIndex.coerceIn(0, numberOfIntervals - 1)
        val n = numberOfIntervals - 1
        val r0 = breathingIntervalDurationMaxSeconds.toDouble()
        val rn = breathingIntervalDurationMinSeconds.toDouble()

        val ratio = i.toDouble() / n.toDouble()
        val factor = Math.pow(ratio, pFactor)
        val duration = r0 + (rn - r0) * factor

        return duration.toInt().coerceAtLeast(breathingIntervalDurationMinSeconds)
    }

    // Total session time in seconds (breath holds + breathing intervals, excluding intro)
    val totalSessionTimeSeconds: Int
        get() {
            // All breath hold intervals
            val totalHoldTime = numberOfIntervals * breathHoldDurationSeconds

            // Breathing intervals (one less than number of holds - no breathing after last hold)
            val totalBreathingTime = if (numberOfIntervals > 1) {
                (0 until numberOfIntervals - 1).sumOf { breathingIntervalDuration(it) }
            } else {
                0
            }

            return totalHoldTime + totalBreathingTime
        }
}
