package com.apneaalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.apneaalarm.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAllAlarms(context)
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        val repository = PreferencesRepository(context)
        val alarmScheduler = AlarmScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            val alarms = repository.alarmsFlow.first()
            alarmScheduler.scheduleAllAlarms(alarms)
        }
    }
}
