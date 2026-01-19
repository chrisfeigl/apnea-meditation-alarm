package com.apneaalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.apneaalarm.session.SessionService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlarmScheduler.ACTION_ALARM_TRIGGER) {
            startSessionService(context)
        }
    }

    private fun startSessionService(context: Context) {
        val serviceIntent = Intent(context, SessionService::class.java).apply {
            action = SessionService.ACTION_START_SESSION
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
