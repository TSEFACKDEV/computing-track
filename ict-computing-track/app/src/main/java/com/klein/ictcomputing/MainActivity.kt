package com.klein.ictcomputing

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.klein.ictcomputing.data.AppRepository
import com.klein.ictcomputing.focus.FocusMode
import com.klein.ictcomputing.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        FocusMode.onResume(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as IctApp
        val openSession = intent.getBooleanExtra(AppRepository.EXTRA_START_SESSION, false)
        if (intent.getBooleanExtra(AppRepository.EXTRA_FROM_ALARM, false)) {
            // Réveil déclenché depuis la notification : s'assurer que le service s'arrête bien.
            com.klein.ictcomputing.alarm.AlarmService.stop(this)
        }
        try {
            // Relances programmées du jour (midi → 22h30) tant que la session n'est pas finie.
            com.klein.ictcomputing.alarm.NagSystem.sync(this)
        } catch (_: Exception) {
        }
        setContent {
            AppRoot(app = app, openSessionAfterAlarm = openSession)
        }
    }

    companion object {
        const val ACTION_OPEN_SESSION = "com.klein.ictcomputing.OPEN_SESSION"
        fun openIntent(context: android.content.Context, fromAlarm: Boolean): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(AppRepository.EXTRA_FROM_ALARM, fromAlarm)
                .putExtra(AppRepository.EXTRA_START_SESSION, fromAlarm)
    }
}
