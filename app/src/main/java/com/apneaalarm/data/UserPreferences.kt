package com.apneaalarm.data

enum class TrainingMode {
    RELAXATION,  // Calm, meditation-focused
    TRAINING     // Performance, CO₂ tolerance training
}

enum class IntensityLevel(val label: String, val description: String) {
    CALM("Calm", "Recovery"),
    CHALLENGING("Challenging", "Controlled"),
    HARD_TRAINING("Hard Training", "Demanding"),
    ADVANCED("Advanced", "Infrequent use")
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

    // Manual interval settings (when useManualIntervalSettings is true)
    val useManualIntervalSettings: Boolean = false,
    val manualBreathHoldDurationSeconds: Int = 36,  // H
    val manualR0Seconds: Int = 45,                   // R0 (max breathing interval)
    val manualRnSeconds: Int = 9,                    // Rn (min breathing interval)
    val manualNumberOfIntervals: Int = 6,            // N
    val manualPFactor: Float = 1.4f,                 // p

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
    // Mode-specific parameters (used when useManualIntervalSettings is false)
    // Relaxation: H = 0.60*M, R0 = 1.25*H, Rn = 0.25*H, N = 6, p = 1.4
    // Intense:    H = 0.90*M, R0 = 0.50*H, Rn = 0.12*H, N = 8, p = 0.75

    private val computedHoldMultiplier: Double
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 0.60
            TrainingMode.TRAINING -> 0.90
        }

    private val computedR0Multiplier: Double
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 1.25
            TrainingMode.TRAINING -> 0.50
        }

    private val computedRnMultiplier: Double
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 0.25
            TrainingMode.TRAINING -> 0.12
        }

    private val computedNumberOfIntervals: Int
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 6
            TrainingMode.TRAINING -> 8
        }

    private val computedPFactor: Double
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 1.4
            TrainingMode.TRAINING -> 0.75
        }

    // Computed H based on mode
    private val computedBreathHoldDurationSeconds: Int
        get() = (maxStaticBreathHoldDurationSeconds * computedHoldMultiplier).toInt()

    // Computed R0 based on mode
    private val computedR0Seconds: Int
        get() = (computedBreathHoldDurationSeconds * computedR0Multiplier).toInt()

    // Computed Rn based on mode
    private val computedRnSeconds: Int
        get() = (computedBreathHoldDurationSeconds * computedRnMultiplier).toInt().coerceAtLeast(3)

    // Effective values (manual or computed based on flag)
    val numberOfIntervals: Int
        get() = if (useManualIntervalSettings) manualNumberOfIntervals else computedNumberOfIntervals

    val breathHoldDurationSeconds: Int
        get() = if (useManualIntervalSettings) manualBreathHoldDurationSeconds else computedBreathHoldDurationSeconds

    val breathingIntervalDurationMaxSeconds: Int
        get() = if (useManualIntervalSettings) manualR0Seconds else computedR0Seconds

    val breathingIntervalDurationMinSeconds: Int
        get() = if (useManualIntervalSettings) manualRnSeconds.coerceAtLeast(3) else computedRnSeconds

    private val effectivePFactor: Double
        get() = if (useManualIntervalSettings) manualPFactor.toDouble() else computedPFactor

    // Calculate breathing interval duration for a given interval index
    // Formula: R0 + (Rn - R0) * (i / (N-1))^p
    fun breathingIntervalDuration(intervalIndex: Int): Int {
        if (numberOfIntervals <= 1) return breathingIntervalDurationMaxSeconds

        val i = intervalIndex.coerceIn(0, numberOfIntervals - 1)
        val n = numberOfIntervals - 1
        val r0 = breathingIntervalDurationMaxSeconds.toDouble()
        val rn = breathingIntervalDurationMinSeconds.toDouble()

        val ratio = i.toDouble() / n.toDouble()
        val factor = Math.pow(ratio, effectivePFactor)
        val duration = r0 + (rn - r0) * factor

        return duration.toInt().coerceAtLeast(breathingIntervalDurationMinSeconds)
    }

    // Total session time in seconds (breath holds + breathing intervals, excluding intro)
    val totalSessionTimeSeconds: Int
        get() {
            // All breath hold intervals
            val totalHoldTime = numberOfIntervals * breathHoldDurationSeconds

            // N breathing intervals (one after each hold, including the last)
            val totalBreathingTime = if (numberOfIntervals > 0) {
                (0 until numberOfIntervals).sumOf { breathingIntervalDuration(it) }
            } else {
                0
            }

            return totalHoldTime + totalBreathingTime
        }

    // Intensity Factor (1-100)
    // Simple score reflecting: hold vs max, recovery shortness, and round count
    val intensityFactor: Int
        get() {
            val m = maxStaticBreathHoldDurationSeconds.toDouble()
            val h = breathHoldDurationSeconds.toDouble()
            val n = numberOfIntervals

            if (m <= 0 || h <= 0 || n <= 1) return 1

            // Calculate average rest interval from actual generated intervals (N intervals)
            val avgRest = (0 until n)
                .map { breathingIntervalDuration(it).toDouble() }
                .average()

            // intensity = 100 * (H/M) * (H/(H+avgRest)) * (N/12)
            val holdRatio = h / m                    // How close hold is to max
            val recoveryStress = h / (h + avgRest)  // Shorter rest = higher stress
            val roundFactor = n.toDouble() / 12.0   // More rounds = harder

            val intensity = 100.0 * holdRatio * recoveryStress * roundFactor
            return intensity.toInt().coerceIn(1, 100)
        }

    // Intensity level category based on intensity factor
    val intensityLevel: IntensityLevel
        get() = when {
            intensityFactor <= 25 -> IntensityLevel.CALM
            intensityFactor <= 50 -> IntensityLevel.CHALLENGING
            intensityFactor <= 75 -> IntensityLevel.HARD_TRAINING
            else -> IntensityLevel.ADVANCED
        }
}
