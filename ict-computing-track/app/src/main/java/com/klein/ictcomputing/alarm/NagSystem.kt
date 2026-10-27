package com.klein.ictcomputing.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.klein.ictcomputing.MainActivity
import com.klein.ictcomputing.data.AppRepository
import com.klein.ictcomputing.data.ProgressStore
import com.klein.ictcomputing.data.SUBJECTS
import java.time.LocalDate
import java.util.Calendar

/**
 * Système de relance « à la Duolingo » :
 *  - notifications harcelantes tout au long de la journée tant que les 3 tâches
 *    (openEuler, openGauss, Kunpeng) ne sont pas validées ;
 *  - ultimatum en soirée (« ta série expire à minuit ») ;
 *  - notification de félicitations quand la journée est bouclée ;
 *  - ré-armement automatique au redémarrage via BootReceiver.
 */
object NagSystem {
    const val ACTION_NAG = "com.klein.ictcomputing.NAG"

    private const val CHANNEL_SOFT = "ict_nag_channel"
    private const val CHANNEL_DEMANDING = "ict_nag_ultimatum"
    private const val ID_NAG = 4243
    private const val ID_DONE = 4244
    private const val PREFS = "ict_nags"
    private const val REQ_BASE = 880

    /** Créneaux de relance (minutes du jour) avec messages en français. */
    private val SLOTS = listOf(
        12 * 60,      // pause du midi
        15 * 60,      // après-midi
        18 * 60,      // fin de journée
        20 * 60,      // début de soirée
        21 * 60,      // ultimatum
        22 * 60 + 30, // dernier avertissement
    )

    fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /* -------------------- État du jour -------------------- */

    private fun today(): String = LocalDate.now().toString()

    /** Nombre de matières déjà validées aujourd'hui (0..3). */
    fun doneToday(context: Context): Int {
        val p = try {
            ProgressStore(context).load()
        } catch (_: Exception) {
            return 3
        }
        return SUBJECTS.count { p.daily["${today()}|$it"] == true }
    }

    /** Streak courant lu directement depuis les prefs de progression. */
    fun currentStreak(context: Context): Int = try {
        ProgressStore(context).load().streak
    } catch (_: Exception) {
        0
    }

    fun dayComplete(context: Context): Boolean = doneToday(context) >= SUBJECTS.size

    /* -------------------- Programmation -------------------- */

    fun schedule(context: Context) {
        cancelPending(context)
        if (dayComplete(context)) return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = Calendar.getInstance()
        var scheduled = 0
        for ((index, minuteOfDay) in SLOTS.withIndex()) {
            val cal = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
                set(Calendar.MINUTE, minuteOfDay % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= now.timeInMillis + 60_000) continue
            val pi = PendingIntent.getBroadcast(
                context,
                REQ_BASE + index,
                Intent(context, NagReceiver::class.java).setAction(ACTION_NAG).putExtra("slot", index),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                scheduled++
            } catch (_: SecurityException) {
                try {
                    am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                    scheduled++
                } catch (_: Exception) {
                }
            }
        }
        prefs(context).edit().putInt("scheduled", scheduled).apply()
    }

    fun cancelPending(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        for (index in SLOTS.indices) {
            val pi = PendingIntent.getBroadcast(
                context,
                REQ_BASE + index,
                Intent(context, NagReceiver::class.java).setAction(ACTION_NAG),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
            )
            if (pi != null) {
                am.cancel(pi)
                pi.cancel()
            }
        }
    }

    /** À appeler à chaque changement de progression : (re)programme et célèbre. */
    fun sync(context: Context) {
        val complete = dayComplete(context)
        if (complete) {
            cancelPending(context)
            if (!prefs(context).getBoolean("celebrated_${today()}", false)) {
                postDone(context)
                prefs(context).edit().putBoolean("celebrated_${today()}", true).apply()
            }
        } else {
            schedule(context)
        }
    }

    /* -------------------- Notifications -------------------- */

    private fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val soft = NotificationChannel(CHANNEL_SOFT, "Relances d'étude", NotificationManager.IMPORTANCE_HIGH)
        soft.description = "Rappels pour finir la session du jour"
        soft.enableVibration(true)
        nm.createNotificationChannel(soft)
        val demanding = NotificationChannel(CHANNEL_DEMANDING, "Ultimatum de série", NotificationManager.IMPORTANCE_HIGH)
        demanding.description = "Rappels insistants quand la série est en danger"
        demanding.enableVibration(true)
        nm.createNotificationChannel(demanding)
    }

    private fun openSessionIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(AppRepository.EXTRA_START_SESSION, true)
            .putExtra(AppRepository.EXTRA_FROM_ALARM, false)
        return PendingIntent.getActivity(
            context,
            890,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nagMessage(context: Context, slot: Int, done: Int, streak: Int): Pair<String, String> {
        val remaining = SUBJECTS.size - done
        val dangerHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 20
        val title: String
        val body: String
        when {
            done == 0 -> when (slot) {
                0 -> {
                    title = "Ta session du jour t'attend"
                    body = "3 sujets n'ont pas été touchés : openEuler, openGauss, Kunpeng. C'est parti en 5 min ?"
                }
                1 -> {
                    title = "Ton vocabulaire Huawei te manque"
                    body = "Ta banque de questions n'attend que toi. Ouvre la session : 5 min suffisent."
                }
                else -> {
                    title = "La série de $streak j va mourir"
                    body = "Aucun chapitre validé aujourd'hui. Relance la session maintenant."
                }
            }
            dangerHour -> {
                title = "⚠️ Série en danger : $remaining sujet(s) restant(s)"
                body = "Il ne reste que quelques heures pour sécuriser ta série de $streak j."
            }
            else -> {
                title = "Encore $remaining matière(s) → journée validée"
                body = "$done/3 déjà fait, série de $streak j intacte. Continue !"
            }
        }
        return title to body
    }

    fun postNag(context: Context, slot: Int) {
        ensureChannels(context)
        val done = doneToday(context)
        if (done >= SUBJECTS.size) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val granted = if (Build.VERSION.SDK_INT >= 33) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true
        if (!granted) return

        val (title, body) = nagMessage(context, slot, done, currentStreak(context))
        val evening = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 20
        val channel = if (evening) CHANNEL_DEMANDING else CHANNEL_SOFT

        val notification: Notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, channel)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(context)
        }
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openSessionIntent(context))
            .build()
        nm.notify(ID_NAG, notification)
    }

    fun postDone(context: Context) {
        ensureChannels(context)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val streak = currentStreak(context)
        val body =
            "Les 3 matières sont validées : openEuler, openGauss, Kunpeng. Série de $streak j sécurisée 💪"
        val notification: Notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, CHANNEL_SOFT)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(context)
        }
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Journée validée 🎉 +100 XP")
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openSessionIntent(context))
            .build()
        nm.notify(ID_DONE, notification)
    }
}

/** Reçoit les relances programmées du jour. */
class NagReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.getIntExtra("slot", 0)
        NagSystem.postNag(context, slot)
    }
}
