package com.klein.ictcomputing.alarm

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.PowerManager

class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var oldVibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                AlarmSystem.setRinging(this, true)
                startAsForeground()
                acquireWake()
                playSound()
                startVibration()
                val utils = getSystemService(AudioManager::class.java)
                try {
                    utils.setStreamVolume(AudioManager.STREAM_ALARM, utils.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
                } catch (_: Exception) {
                }
            }
            ACTION_STOP, ACTION_FORCE_STOP -> {
                AlarmSystem.setRinging(this, false)
                stopRingtone()
                AlarmNotifications.cancel(this)
                AlarmSystem.cancelRepeat(this)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val notif: Notification = buildForegroundNotification()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(AlarmNotifications.NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else if (Build.VERSION.SDK_INT >= 29) {
                startForeground(AlarmNotifications.NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(AlarmNotifications.NOTIF_ID, notif)
            }
        } catch (e: Exception) {
            try {
                startForeground(AlarmNotifications.NOTIF_ID, notif)
            } catch (_: Exception) {
            }
        }
    }

    private fun buildForegroundNotification(): Notification {
        AlarmNotifications.createChannel(this)
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            android.app.Notification.Builder(this, AlarmNotifications.CHANNEL_ALARM)
        } else {
            @Suppress("DEPRECATION") android.app.Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Réveil actif — session du jour")
            .setContentText("Ouvre l'application pour arrêter le réveil et étudier.")
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_MAX)
            .build()
    }

    private fun acquireWake() {
        val pm = getSystemService(PowerManager::class.java) ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ICTComputingTrack:alarm").also {
            it.acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWake() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    private fun playSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                if (Build.VERSION.SDK_INT >= 26) {
                    setDataSource(this@AlarmService, uri)
                } else {
                    setDataSource(uri.toString())
                }
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            try {
                player = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                player?.isLooping = true
                player?.start()
            } catch (_: Exception) {
            }
        }
    }

    private fun startVibration() {
        try {
            val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
                val vm = getSystemService(VibratorManager::class.java)
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            oldVibrator = v
            val pattern = longArrayOf(0, 700, 400, 700, 400)
            v.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } catch (_: Exception) {
        }
    }

    private fun stopRingtone() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
        try {
            oldVibrator?.cancel()
        } catch (_: Exception) {
        }
        oldVibrator = null
        releaseWake()
    }

    override fun onDestroy() {
        stopRingtone()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.klein.ictcomputing.alarm.ACTION_START"
        const val ACTION_STOP = "com.klein.ictcomputing.alarm.ACTION_STOP"
        const val ACTION_FORCE_STOP = "com.klein.ictcomputing.alarm.ACTION_FORCE_STOP"

        fun stop(context: Context) {
            val i = Intent(context, AlarmService::class.java).setAction(ACTION_STOP)
            try {
                context.startService(i)
            } catch (_: Exception) {
            }
        }
    }
}
