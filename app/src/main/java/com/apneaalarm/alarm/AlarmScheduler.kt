package com.apneaalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.apneaalarm.data.Alarm
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val SNOOZE_REQUEST_CODE = 9999
        const val ACTION_ALARM_TRIGGER = "com.apneaalarm.ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_SNOOZE_DURATION = "extra_snooze_duration"
        const val EXTRA_SNOOZE_ENABLED = "extra_snooze_enabled"

        // Generate unique request code for each alarm based on its ID
        fun getRequestCodeForAlarm(alarmId: Long): Int {
            // Use modulo to fit in Int range, add offset to avoid collision with snooze code
            return ((alarmId % 9000) + 1000).toInt()
        }
    }

    /**
     * Schedule an alarm for the next valid occurrence based on its days and time.
     */
    fun scheduleAlarm(alarm: Alarm) {
        if (!alarm.enabled || alarm.days.isEmpty()) {
            cancelAlarm(alarm.id)
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_SNOOZE_DURATION, alarm.snoozeDurationMinutes)
            putExtra(EXTRA_SNOOZE_ENABLED, alarm.snoozeEnabled)
        }

        val requestCode = getRequestCodeForAlarm(alarm.id)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = calculateNextTriggerTime(alarm)
        if (triggerTime == null) {
            cancelAlarm(alarm.id)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                    pendingIntent
                )
            }
        } else {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
        }
    }

    /**
     * Calculate the next trigger time for an alarm based on its days and time.
     */
    private fun calculateNextTriggerTime(alarm: Alarm): Long? {
        if (alarm.days.isEmpty()) return null

        val now = Calendar.getInstance()
        val currentDayOfWeek = when (now.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        // Check if alarm time has passed today
        val alarmTimeToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val alarmPassedToday = alarmTimeToday.timeInMillis <= now.timeInMillis

        // Find next alarm day
        for (daysAhead in 0..7) {
            val checkDay = ((currentDayOfWeek - 1 + daysAhead) % 7) + 1

            // Skip today if alarm already passed
            if (daysAhead == 0 && alarmPassedToday) continue

            if (checkDay in alarm.days) {
                val triggerCalendar = Calendar.getInstance().apply {
                    timeInMillis = now.timeInMillis
                    add(Calendar.DAY_OF_YEAR, daysAhead)
                    set(Calendar.HOUR_OF_DAY, alarm.hour)
                    set(Calendar.MINUTE, alarm.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return triggerCalendar.timeInMillis
            }
        }

        return null
    }

    /**
     * Cancel an alarm by its ID.
     */
    fun cancelAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }

        val requestCode = getRequestCodeForAlarm(alarmId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Schedule a snooze alarm that will trigger after the specified minutes.
     * Uses a fixed request code since only one snooze can be active at a time.
     */
    fun scheduleSnooze(
        minutes: Int,
        alarmId: Long,
        snoozeDurationMinutes: Int,
        snoozeEnabled: Boolean
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_SNOOZE_DURATION, snoozeDurationMinutes)
            putExtra(EXTRA_SNOOZE_ENABLED, snoozeEnabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SNOOZE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                    pendingIntent
                )
            }
        } else {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                pendingIntent
            )
        }
    }

    /**
     * Cancel any pending snooze alarm.
     */
    fun cancelSnooze() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SNOOZE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Schedule all enabled alarms.
     */
    fun scheduleAllAlarms(alarms: List<Alarm>) {
        alarms.forEach { alarm ->
            if (alarm.enabled) {
                scheduleAlarm(alarm)
            } else {
                cancelAlarm(alarm.id)
            }
        }
    }

    fun getNextAlarmTime(): Long? {
        val alarmClockInfo = alarmManager.nextAlarmClock
        return alarmClockInfo?.triggerTime
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
