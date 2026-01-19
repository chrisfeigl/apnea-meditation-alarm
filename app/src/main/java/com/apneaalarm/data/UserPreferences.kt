package com.apneaalarm.data

enum class TrainingMode {
    RELAXATION,  // Calm, meditation-focused
    INTENSE      // Performance, CO₂ tolerance training
}

enum class IntensityLevel(val label: String, val description: String) {
    CALM("Calm", "Recovery focused"),
    CHALLENGING("Challenging", "Controlled stress"),
    HIGH_STRESS("High Stress", "Adaptation training"),
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
            TrainingMode.INTENSE -> 0.90
        }

    private val computedR0Multiplier: Double
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 1.25
            TrainingMode.INTENSE -> 0.50
        }

    private val computedRnMultiplier: Double
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 0.25
            TrainingMode.INTENSE -> 0.12
        }

    private val computedNumberOfIntervals: Int
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 6
            TrainingMode.INTENSE -> 8
        }

    private val computedPFactor: Double
        get() = when (trainingMode) {
            TrainingMode.RELAXATION -> 1.4
            TrainingMode.INTENSE -> 0.75
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

            // Breathing intervals (one less than number of holds - no breathing after last hold)
            val totalBreathingTime = if (numberOfIntervals > 1) {
                (0 until numberOfIntervals - 1).sumOf { breathingIntervalDuration(it) }
            } else {
                0
            }

            return totalHoldTime + totalBreathingTime
        }

    // Intensity Factor (1-100)
    // Combines hold load, recovery compression, density, and curve aggression
    val intensityFactor: Int
        get() {
            val m = maxStaticBreathHoldDurationSeconds.toDouble()
            val h = breathHoldDurationSeconds.toDouble()
            val rn = breathingIntervalDurationMinSeconds.toDouble()
            val n = numberOfIntervals.toDouble()
            val p = effectivePFactor

            // Hold Load: how close the training hold is to max (0.5-1.0 range typically)
            val holdLoad = if (m > 0) h / m else 0.0

            // Recovery Compression: how short the min rest is relative to hold (higher = more intense)
            val recoveryCompression = if (h > 0) 1.0 - (rn / h) else 0.0

            // Density Factor: number of rounds normalized to 10 as upper bound
            val densityFactor = (n / 10.0).coerceAtMost(1.0)

            // Curve Aggression: lower p = earlier stress = higher intensity
            // CA = clamp((1.6 - p) / (1.6 - 0.5), 0, 1)
            val curveAggression = ((1.6 - p) / (1.6 - 0.5)).coerceIn(0.0, 1.0)

            // Weighted combination
            val intensityRaw = 0.40 * holdLoad +
                    0.30 * recoveryCompression +
                    0.20 * densityFactor +
                    0.10 * curveAggression

            return (100 * intensityRaw).toInt().coerceIn(1, 100)
        }

    // Intensity level category based on intensity factor
    val intensityLevel: IntensityLevel
        get() = when {
            intensityFactor < 40 -> IntensityLevel.CALM
            intensityFactor < 70 -> IntensityLevel.CHALLENGING
            intensityFactor < 90 -> IntensityLevel.HIGH_STRESS
            else -> IntensityLevel.ADVANCED
        }
}
