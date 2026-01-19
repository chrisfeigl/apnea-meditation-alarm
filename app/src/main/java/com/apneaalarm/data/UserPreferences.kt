package com.apneaalarm.data

data class UserPreferences(
    // Alarm settings
    val alarmHour: Int = 7,
    val alarmMinute: Int = 0,
    val alarmEnabled: Boolean = false,
    val isFirstTimeSetupComplete: Boolean = false,

    // Breath hold settings
    val maxStaticBreathHoldDurationSeconds: Int = 60,

    // Volume multipliers (1-10 scale, default 10 = 100% of system alarm volume)
    val introBowlVolumeMultiplier: Int = 10,
    val breathChimeVolumeMultiplier: Int = 10,
    val holdChimeVolumeMultiplier: Int = 10,

    // Breathing interval settings
    val breathingIntervalDurationMaxSeconds: Int = 60,
    val breathingIntervalDurationMinSeconds: Int = 3,
    val numberOfIntervals: Int = 10,
    val pFactor: Float = 0.25f,

    // Custom sound file URIs (null = use default bundled sounds)
    val customIntroBowlUri: String? = null,
    val customBreathChimeUri: String? = null,
    val customHoldChimeUri: String? = null,

    // Intro bowl fade-in option (true = fade in gradually, false = play at full volume immediately)
    val fadeInIntroBowl: Boolean = true,

    // Snooze duration in minutes
    val snoozeDurationMinutes: Int = 5
) {
    // Computed: breath hold duration is 75% of max static hold
    val breathHoldDurationSeconds: Int
        get() = (maxStaticBreathHoldDurationSeconds * 0.75).toInt()

    // Calculate breathing interval duration for a given interval index
    fun breathingIntervalDuration(intervalIndex: Int): Int {
        if (numberOfIntervals <= 1) return breathingIntervalDurationMaxSeconds

        val i = intervalIndex.coerceIn(0, numberOfIntervals - 1)
        val n = numberOfIntervals - 1
        val max = breathingIntervalDurationMaxSeconds.toDouble()
        val min = breathingIntervalDurationMinSeconds.toDouble()

        // Formula: max + (min - max) * (i / (N-1))^pFactor
        val ratio = i.toDouble() / n.toDouble()
        val factor = Math.pow(ratio, pFactor.toDouble())
        val duration = max + (min - max) * factor

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
