package com.apneaalarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ApneaAlarmApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sessionChannel = NotificationChannel(
                "apnea_alarm_session",
                "Breathing Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress during breathing sessions"
                setShowBadge(false)
            }

            val alarmChannel = NotificationChannel(
                "apnea_alarm_alarm",
                "Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm notifications"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(sessionChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }
}
