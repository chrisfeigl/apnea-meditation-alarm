package com.apneaalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.apneaalarm.session.SessionService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlarmScheduler.ACTION_ALARM_TRIGGER) {
            val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
            val snoozeDuration = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_DURATION, 5)
            val snoozeEnabled = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE_ENABLED, true)
            startSessionService(context, alarmId, snoozeDuration, snoozeEnabled)
        }
    }

    private fun startSessionService(
        context: Context,
        alarmId: Long,
        snoozeDuration: Int,
        snoozeEnabled: Boolean
    ) {
        val serviceIntent = Intent(context, SessionService::class.java).apply {
            action = SessionService.ACTION_START_SESSION
            putExtra(SessionService.EXTRA_ALARM_ID, alarmId)
            putExtra(SessionService.EXTRA_SNOOZE_DURATION, snoozeDuration)
            putExtra(SessionService.EXTRA_SNOOZE_ENABLED, snoozeEnabled)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
