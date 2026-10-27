package com.klein.ictcomputing.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmSystem {
    const val ACTION_ALARM = "com.klein.ictcomputing.ACTION_ALARM"
    const val ACTION_STOP = "com.klein.ictcomputing.ACTION_STOP_ALARM"
    private const val REQUEST_ALARM = 777
    private const val REQUEST_REPEAT = 778

    fun prefs(context: Context) =
        context.getSharedPreferences("ict_alarm", Context.MODE_PRIVATE)

    fun alarmHour(context: Context): Int = prefs(context).getInt("hour", 3)
    fun alarmMinute(context: Context): Int = prefs(context).getInt("minute", 0)
    fun enabled(context: Context): Boolean = prefs(context).getBoolean("enabled", true)
    fun isRinging(context: Context): Boolean =
        prefs(context).getBoolean("ringing", false)

    fun setRinging(context: Context, ringing: Boolean) {
        prefs(context).edit().putBoolean("ringing", ringing).apply()
    }

    fun setAlarmTime(context: Context, hour: Int, minute: Int) {
        prefs(context).edit().putInt("hour", hour).putInt("minute", minute).apply()
        schedule(context)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("enabled", enabled).apply()
        if (enabled) schedule(context) else cancel(context)
    }

    fun nextTriggerMillis(context: Context): Long {
        val hour = alarmHour(context)
        val minute = alarmMinute(context)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_ALARM),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun repeatIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_REPEAT,
        Intent(context, AlarmReceiver::class.java).setAction("com.klein.ictcomputing.ACTION_REPEAT_ALARM"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(alarmIntent(context))
    }

    fun cancelRepeat(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(repeatIntent(context))
    }

    fun schedule(context: Context) {
        if (!enabled(context)) return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val trigger = nextTriggerMillis(context)
        val pi = alarmIntent(context)
        val canExact = if (Build.VERSION.SDK_INT >= 31) am.canScheduleExactAlarms() else true
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, trigger, 60_000L, pi)
        }
        prefs(context).edit().putLong("next", trigger).apply()
    }

    fun scheduleRepeat(context: Context, delayMs: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val alarmCount = prefs(context).getInt("repeat_count", 0)
        if (alarmCount >= 15) return
        prefs(context).edit()
            .putInt("repeat_count", alarmCount + 1)
            .putLong("repeat_at", System.currentTimeMillis() + delayMs)
            .apply()
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + delayMs,
            repeatIntent(context),
        )
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmSystem.ACTION_ALARM -> fire(context, initial = true)
            "com.klein.ictcomputing.ACTION_REPEAT_ALARM" -> fire(context, initial = false)
        }
    }

    private fun fire(context: Context, initial: Boolean) {
        AlarmSystem.setRinging(context, true)
        AlarmNotifications.show(context)
        AlarmSystem.scheduleRepeat(context, 90_000)
        try {
            context.startForegroundService(
                Intent(context, AlarmService::class.java).setAction(AlarmService.ACTION_START),
            )
        } catch (_: Exception) {
        }
        AlarmNotifications.show(context)
        AlarmSystem.schedule(context)
    }
}
