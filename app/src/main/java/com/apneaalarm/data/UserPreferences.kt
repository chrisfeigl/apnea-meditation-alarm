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

/**
 * Shared session settings used by both Alarm and SavedSession.
 * Note: Global M (maxStaticBreathHoldDurationSeconds) is stored in UserPreferences
 * and must be passed to computed property methods.
 */
data class SessionSettings(
    // Intention
    val trainingMode: TrainingMode = TrainingMode.RELAXATION,
    // Manual intervals
    val useManualIntervalSettings: Boolean = false,
    val manualBreathHoldDurationSeconds: Int = 36,  // H
    val manualR0Seconds: Int = 45,                   // R0 (max breathing interval)
    val manualRnSeconds: Int = 9,                    // Rn (min breathing interval)
    val manualNumberOfIntervals: Int = 6,            // N
    val manualPFactor: Float = 1.4f,                 // p
    // Audio
    val introBowlVolumeMultiplier: Int = 10,
    val breathChimeVolumeMultiplier: Int = 10,
    val holdChimeVolumeMultiplier: Int = 10,
    val customIntroBowlUri: String? = null,
    val customBreathChimeUri: String? = null,
    val customHoldChimeUri: String? = null,
    val fadeInIntroBowl: Boolean = true
) {
    // Mode-specific parameters (used when useManualIntervalSettings is false)
    // Relaxation: H = 0.60*M, R0 = 1.25*H, Rn = 0.25*H, N = 6, p = 1.4
    // Training:   H = 0.90*M, R0 = 0.50*H, Rn = 0.12*H, N = 8, p = 0.75

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

    // Computed H based on mode (requires global M)
    private fun computedBreathHoldDurationSeconds(globalM: Int): Int =
        (globalM * computedHoldMultiplier).toInt()

    // Computed R0 based on mode
    private fun computedR0Seconds(globalM: Int): Int =
        (computedBreathHoldDurationSeconds(globalM) * computedR0Multiplier).toInt()

    // Computed Rn based on mode
    private fun computedRnSeconds(globalM: Int): Int =
        (computedBreathHoldDurationSeconds(globalM) * computedRnMultiplier).toInt().coerceAtLeast(3)

    // Effective values (manual or computed based on flag)
    fun numberOfIntervals(): Int =
        if (useManualIntervalSettings) manualNumberOfIntervals else computedNumberOfIntervals

    fun breathHoldDurationSeconds(globalM: Int): Int =
        if (useManualIntervalSettings) manualBreathHoldDurationSeconds else computedBreathHoldDurationSeconds(globalM)

    fun breathingIntervalDurationMaxSeconds(globalM: Int): Int =
        if (useManualIntervalSettings) manualR0Seconds else computedR0Seconds(globalM)

    fun breathingIntervalDurationMinSeconds(globalM: Int): Int =
        if (useManualIntervalSettings) manualRnSeconds.coerceAtLeast(3) else computedRnSeconds(globalM)

    private fun effectivePFactor(): Double =
        if (useManualIntervalSettings) manualPFactor.toDouble() else computedPFactor

    // Calculate breathing interval duration for a given interval index
    // Formula: R0 + (Rn - R0) * (i / (N-1))^p
    fun breathingIntervalDuration(intervalIndex: Int, globalM: Int): Int {
        val n = numberOfIntervals()
        if (n <= 1) return breathingIntervalDurationMaxSeconds(globalM)

        val i = intervalIndex.coerceIn(0, n - 1)
        val nMinus1 = n - 1
        val r0 = breathingIntervalDurationMaxSeconds(globalM).toDouble()
        val rn = breathingIntervalDurationMinSeconds(globalM).toDouble()

        val ratio = i.toDouble() / nMinus1.toDouble()
        val factor = Math.pow(ratio, effectivePFactor())
        val duration = r0 + (rn - r0) * factor

        return duration.toInt().coerceAtLeast(breathingIntervalDurationMinSeconds(globalM))
    }

    // Total session time in seconds (breath holds + breathing intervals, excluding intro)
    fun totalSessionTimeSeconds(globalM: Int): Int {
        val n = numberOfIntervals()
        // All breath hold intervals
        val totalHoldTime = n * breathHoldDurationSeconds(globalM)

        // N breathing intervals (one after each hold, including the last)
        val totalBreathingTime = if (n > 0) {
            (0 until n).sumOf { breathingIntervalDuration(it, globalM) }
        } else {
            0
        }

        return totalHoldTime + totalBreathingTime
    }

    // Intensity Factor (1-100)
    fun intensityFactor(globalM: Int): Int {
        val m = globalM.toDouble()
        val h = breathHoldDurationSeconds(globalM).toDouble()
        val n = numberOfIntervals()

        if (m <= 0 || h <= 0 || n <= 1) return 1

        // Calculate average rest interval from actual generated intervals (N intervals)
        val avgRest = (0 until n)
            .map { breathingIntervalDuration(it, globalM).toDouble() }
            .average()

        // intensity = 100 * (H/M) * (H/(H+avgRest)) * (N/12)
        val holdRatio = h / m                    // How close hold is to max
        val recoveryStress = h / (h + avgRest)  // Shorter rest = higher stress
        val roundFactor = n.toDouble() / 12.0   // More rounds = harder

        val intensity = 100.0 * holdRatio * recoveryStress * roundFactor
        return intensity.toInt().coerceIn(1, 100)
    }

    // Intensity level category based on intensity factor
    fun intensityLevel(globalM: Int): IntensityLevel = when {
        intensityFactor(globalM) <= 25 -> IntensityLevel.CALM
        intensityFactor(globalM) <= 50 -> IntensityLevel.CHALLENGING
        intensityFactor(globalM) <= 75 -> IntensityLevel.HARD_TRAINING
        else -> IntensityLevel.ADVANCED
    }
}

/**
 * A scheduled alarm with its own time, days, snooze, and session settings.
 */
data class Alarm(
    val id: Long = System.currentTimeMillis(),
    // Schedule
    val hour: Int = 7,
    val minute: Int = 0,
    val enabled: Boolean = true,
    val days: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // Days of week (1=Mon, 7=Sun)
    // Snooze
    val snoozeEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 5,
    // Session settings
    val sessionSettings: SessionSettings = SessionSettings()
) {
    // Get the next alarm occurrence (returns pair of day name and time string)
    // Returns null if alarm is disabled or no days are selected
    fun getNextAlarmInfo(): Pair<String, String>? {
        if (!enabled || days.isEmpty()) return null

        val calendar = java.util.Calendar.getInstance()
        val currentDayOfWeek = when (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            java.util.Calendar.SUNDAY -> 7
            else -> 1
        }
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)

        // Check if alarm time has passed today
        val alarmPassedToday = currentHour > hour ||
            (currentHour == hour && currentMinute >= minute)

        // Find next alarm day
        for (daysAhead in 0..7) {
            val checkDay = ((currentDayOfWeek - 1 + daysAhead) % 7) + 1

            // Skip today if alarm already passed
            if (daysAhead == 0 && alarmPassedToday) continue

            if (checkDay in days) {
                val dayName = when {
                    daysAhead == 0 -> "Today"
                    daysAhead == 1 -> "Tomorrow"
                    else -> when (checkDay) {
                        1 -> "Monday"
                        2 -> "Tuesday"
                        3 -> "Wednesday"
                        4 -> "Thursday"
                        5 -> "Friday"
                        6 -> "Saturday"
                        7 -> "Sunday"
                        else -> ""
                    }
                }
                val timeString = String.format("%02d:%02d", hour, minute)
                return Pair(dayName, timeString)
            }
        }

        return null
    }
}

/**
 * A named saved session configuration (without schedule) for manual starts.
 */
data class SavedSession(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val sessionSettings: SessionSettings = SessionSettings()
)

/**
 * Global user preferences.
 * Most session-specific settings have moved to SessionSettings (in Alarm and SavedSession).
 */
data class UserPreferences(
    val maxStaticBreathHoldDurationSeconds: Int = 60,  // Global M
    val isFirstTimeSetupComplete: Boolean = false,
    val lastSessionSettings: SessionSettings? = null   // For "Repeat Last Session"
)
