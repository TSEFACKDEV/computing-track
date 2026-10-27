package com.klein.ictcomputing.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klein.ictcomputing.MainActivity
import com.klein.ictcomputing.ui.ContestColors
import com.klein.ictcomputing.ui.subjectColor
import kotlinx.coroutines.delay

/**
 * Écran de réveil plein écran : son continu + vibration + relance automatique
 * tant que la session n'a pas été confirmée.
 */
class AlarmActivity : ComponentActivity() {

    private var dismissed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFlags()
        try {
        AlarmService.stop(this)
        } catch (e: Exception) {
        }
        try {
        startForegroundService(Intent(this, AlarmService::class.java).setAction(AlarmService.ACTION_START))
        } catch (e: Exception) {
        }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF08090D),
                    surface = Color(0xFF101219),
                    primary = ContestColors.AccentRed,
                ),
            ) {
                AlarmLockScreen(onStop = { stopAlarmAndOpenSession() })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        initFlags()
    }

    override fun onStart() {
        super.onStart()
        initFlags()
    }

    override fun onPause() {
        super.onPause()
        if (!dismissed) {
            AlarmNotifications.show(this)
            AlarmSystem.scheduleRepeat(this, 60_000)
        }
    }

    private fun initFlags() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km != null && km.isKeyguardLocked) {
            @Suppress("DEPRECATION")
            km.requestDismissKeyguard(this, null)
        }
    }

    private fun stopAlarmAndOpenSession() {
        dismissed = true
        AlarmService.stop(this)
        AlarmNotifications.cancel(this)
        AlarmSystem.cancelRepeat(this)
        try {
            NagSystem.sync(this)
        } catch (_: Exception) {
        }
        startActivity(MainActivity.openIntent(this, fromAlarm = true))
        finish()
    }

    companion object {
        @JvmStatic
        fun launch(context: Context) {
            val intent = Intent(context, AlarmActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}

@Composable
private fun AlarmLockScreen(onStop: () -> Unit) {
    var now by remember { mutableStateOf("") }
    var challengeText by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var solved by remember { mutableStateOf(false) }
    var triedOnce by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }
    var holding by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val c = java.util.Calendar.getInstance()
            now = String.format("%02d:%02d", c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
            delay(5_000)
        }
    }

    LaunchedEffect(Unit) {
        val a = (6..14).random()
        val b = (3..9).random()
        val c = (10..99).random()
        challengeText = "$a × $b + $c"
    }

    val evalResult: Int? = try {
        if (challengeText.isBlank()) null else {
            val left = challengeText.substringBefore("+").trim()
            val right = challengeText.substringAfter("+").trim().toInt()
            val (x, y) = left.split("×").map { it.trim().toInt() }
            x * y + right
        }
    } catch (e: Exception) {
        null
    }

    LaunchedEffect(holding, solved) {
        if (holding && solved) {
            val start = System.currentTimeMillis()
            val duration = 3500L
            while (holding && solved && holdProgress < 1f) {
                val elapsed = System.currentTimeMillis() - start
                holdProgress = (elapsed / duration.toFloat()).coerceIn(0f, 1f)
                delay(50)
                if (holdProgress >= 1f) {
                    onStop()
                    return@LaunchedEffect
                }
            }
            if (!holding) holdProgress = 0f
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF08090D))
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Text("RÉVEIL ICT COMPUTING", color = ContestColors.AccentRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(now, color = ContestColors.OnSurface, fontSize = 62.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Session d'étude du jour — Computing Track",
                color = ContestColors.OnSurface.copy(alpha = 0.75f),
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(22.dp))
            Text("3 matières à maîtriser aujourd'hui", color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            listOf(
                Triple("openEuler", "Cours du jour", "50 %"),
                Triple("openGauss", "Cours + QCM", "30 %"),
                Triple("Kunpeng", "Cours + QCM", "20 %"),
            ).forEach { (subject, task, weight) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(Color(0xFF12141C), MaterialTheme.shapes.large)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("●", color = subjectColor(subject), fontSize = 15.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(subject, color = ContestColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("$task · $weight", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(30.dp))

            if (!solved) {
                Text(
                    "Anti-sommeil : résous ce calcul pour déverrouiller.",
                    color = ContestColors.AccentRed,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "$challengeText = ?",
                    color = ContestColors.OnSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Ta réponse") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    keyboardActions = KeyboardActions(onDone = { }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Valider",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ContestColors.AccentRed, MaterialTheme.shapes.large)
                        .padding(vertical = 12.dp)
                        .clickable {
                            triedOnce = true
                            if (evalResult != null && answer.trim().toIntOrNull() == evalResult) {
                                solved = true
                            }
                        },
                )
                if (triedOnce && !solved) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (answer.isBlank()) "Saisis une réponse." else "Faux ! Réessaie : calcule tranquillement.",
                        color = ContestColors.AccentAmber,
                        fontSize = 12.sp,
                    )
                }
            } else {
                Text(
                    "Appuie 3,5 s pour arrêter et ouvrir la session.",
                    color = ContestColors.AccentGreen,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(
                            if (holdProgress > 0f) ContestColors.AccentGreen.copy(alpha = 0.85f) else ContestColors.AccentGreen,
                            MaterialTheme.shapes.large,
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = {
                                holding = true
                                try {
                                    awaitRelease()
                                } finally {
                                    holding = false
                                }
                            })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "MAINTENIR POUR ARRÊTER",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { holdProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = ContestColors.AccentGreen,
                    trackColor = Color(0x1F34D399),
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Le réveil revient toutes les 90 s tant qu'il n'est pas arrêté.",
                color = ContestColors.OnSurfaceMuted,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}
