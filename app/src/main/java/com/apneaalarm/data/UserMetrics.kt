package com.apneaalarm.data

/**
 * Record of a single completed session for metrics tracking.
 */
data class SessionRecord(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val cyclesCompleted: Int,
    val cyclesPlanned: Int,
    val breathHoldDurationSeconds: Int,
    val trainingMode: TrainingMode,
    val wasCompleted: Boolean,
    val intensityFactor: Int
)

/**
 * Aggregate metrics computed from session history.
 */
data class UserMetrics(
    // Totals
    val totalSessions: Int = 0,
    val totalCompletedSessions: Int = 0,
    val totalPracticeTimeSeconds: Long = 0,

    // Streaks
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastSessionDate: Long? = null,

    // This week/month
    val sessionsThisWeek: Int = 0,
    val sessionsThisMonth: Int = 0,
    val practiceTimeThisWeekSeconds: Long = 0,
    val practiceTimeThisMonthSeconds: Long = 0,

    // Performance
    val completionRate: Float = 0f,
    val averageSessionDurationSeconds: Int = 0,
    val averageIntensityFactor: Int = 0,

    // Trends
    val recentBreathHoldAverage: Int = 0,
    val olderBreathHoldAverage: Int = 0,
    val breathHoldTrend: TrendDirection = TrendDirection.STABLE
)

enum class TrendDirection {
    IMPROVING,
    STABLE,
    DECLINING
}
