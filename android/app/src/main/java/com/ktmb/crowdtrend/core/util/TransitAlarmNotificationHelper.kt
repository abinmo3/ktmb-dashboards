package com.ktmb.crowdtrend.core.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ktmb.crowdtrend.MainActivity
import com.ktmb.crowdtrend.R

object TransitAlarmNotificationHelper {

    private const val CHANNEL_ID = "transit_alarms"
    private const val CHANNEL_NAME = "Transit Alarms"
    const val FOREGROUND_CHANNEL_ID = "transit_alarm_service"
    const val FOREGROUND_CHANNEL_NAME = "Transit Alarm Service"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you're approaching a station"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
            }
            val fgChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID, FOREGROUND_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Transit alarm monitoring service"
                setShowBadge(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(alarmChannel)
            nm.createNotificationChannel(fgChannel)
        }
    }

    fun showAlarmNotification(context: Context, stationName: String, label: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("alarm_station", stationName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, stationName.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (label.isNotEmpty()) label else stationName
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📍 Approaching $title")
            .setContentText("You're near $stationName — time to get ready!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(stationName.hashCode(), notification)
    }

    fun buildForegroundNotification(context: Context, activeCount: Int): android.app.Notification {
        return NotificationCompat.Builder(context, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Transit Alarms Active")
            .setContentText("$activeCount alarm${if (activeCount != 1) "s" else ""} monitoring")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
