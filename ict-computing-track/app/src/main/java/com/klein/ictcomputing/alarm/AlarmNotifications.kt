package com.klein.ictcomputing.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmNotifications {
    const val CHANNEL_ALARM = "ict_alarm_channel"
    const val NOTIF_ID = 4242

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ALARM,
            "Réveil d'étude ICT Computing",
            NotificationManager.IMPORTANCE_HIGH,
        )
        channel.description = "Réveil obligatoire de la session d'étude quotidienne"
        channel.setSound(null, null)
        channel.enableVibration(false)
        channel.setBypassDnd(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        nm.createNotificationChannel(channel)
    }

    fun show(context: Context) {
        createChannel(context)
        val activityIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            991,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            android.app.Notification.Builder(context, CHANNEL_ALARM)
        } else {
            @Suppress("DEPRECATION") android.app.Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("ICT Computing — session du jour")
            .setContentText("Ton réveil sonne. Déverrouille pour commencer ta session.")
            .setPriority(Notification.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.notify(NOTIF_ID, notification)
        } catch (_: Exception) {
        }
    }

    fun cancel(context: Context) {
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.cancel(NOTIF_ID)
        } catch (_: Exception) {
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.intent.action.MY_PACKAGE_REPLACED",
            -> {
                if (AlarmSystem.enabled(context)) AlarmSystem.schedule(context)
                NagSystem.sync(context)
            }
        }
    }
}
