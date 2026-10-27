package com.klein.ictcomputing.focus

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Mode concentration « kiosque ».
 *
 * Tant qu'un chapitre n'est pas validé, l'application :
 *  - se épingle avec [Activity.startLockTask] (écran épinglé : Home/Récents bloqués) ;
 *  - masque les barres système ;
 *  - passe le téléphone en Ne pas déranger si l'autorisation est accordée ;
 *  - bloque le bouton Retour.
 *
 * Le mode se termine uniquement quand le quiz du chapitre est validé (>= 60 %).
 */
object FocusMode {
    private const val PREFS = "ict_focus_mode"
    private const val KEY_ACTIVE = "active"
    private const val KEY_CHAPTER = "chapter_id"
    private const val KEY_DND_WAS = "dnd_previous"

    /** État observable par Compose (mis à jour par [begin]/[finish]/[onResume]). */
    var active by mutableStateOf(false)
        private set

    /** Identifiant du chapitre verrouillé, observable par Compose. */
    var lockedChapter by mutableStateOf<String?>(null)
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isActive(context: Context): Boolean = prefs(context).getBoolean(KEY_ACTIVE, false)

    fun sync(context: Context) {
        active = isActive(context)
        lockedChapter = activeChapter(context)
    }

    fun activeChapter(context: Context): String? =
        prefs(context).getString(KEY_CHAPTER, null)

    /** Démarre le mode pour un chapitre non encore validé. */
    fun begin(activity: Activity, chapterId: String) {
        prefs(activity).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_CHAPTER, chapterId)
            .apply()
        active = true
        lockedChapter = chapterId
        enableDoNotDisturb(activity)
        enterLock(activity)
    }

    /** À appeler depuis onResume : ré-épingle l'écran si le mode est encore actif. */
    fun onResume(activity: Activity) {
        sync(activity)
        if (active) {
            enableDoNotDisturb(activity)
            enterLock(activity)
        }
    }

    /** Termine le mode (chapitre validé ou réinitialisation explicite). */
    fun finish(activity: Activity) {
        prefs(activity).edit()
            .putBoolean(KEY_ACTIVE, false)
            .remove(KEY_CHAPTER)
            .apply()
        active = false
        lockedChapter = null
        exitLock(activity)
        restoreDoNotDisturb(activity)
    }

    private fun enterLock(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.startLockTask()
            }
        } catch (_: Exception) {
            // Certains constructeurs refusent l'épinglage : le BackHandler reste actif.
        }
        hideSystemBars(activity)
    }

    private fun exitLock(activity: Activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.stopLockTask()
            }
        } catch (_: Exception) {
        }
        showSystemBars(activity)
    }

    private fun hideSystemBars(activity: Activity) {
        val window = activity.window ?: return
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemBars(activity: Activity) {
        val window = activity.window ?: return
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    private fun enableDoNotDisturb(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (!nm.isNotificationPolicyAccessGranted) return
        try {
            if (!prefs(context).contains(KEY_DND_WAS)) {
                prefs(context).edit()
                    .putInt(KEY_DND_WAS, nm.currentInterruptionFilter)
                    .apply()
            }
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        } catch (_: Exception) {
        }
    }

    private fun restoreDoNotDisturb(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val previous = prefs(context).getInt(KEY_DND_WAS, Int.MIN_VALUE)
        if (previous == Int.MIN_VALUE) return
        try {
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(previous)
            }
        } catch (_: Exception) {
        } finally {
            prefs(context).edit().remove(KEY_DND_WAS).apply()
        }
    }
}
