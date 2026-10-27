package com.klein.ictcomputing.ui

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klein.ictcomputing.alarm.AlarmSystem
import com.klein.ictcomputing.data.AppRepository
import com.klein.ictcomputing.data.SUBJECT_WEIGHTS

@Composable
fun OnboardingScreen(
    selectedPhase: String,
    onSelectPhase: (String) -> Unit,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    var alarmHour by remember { mutableStateOf(AlarmSystem.alarmHour(context)) }
    var alarmMinute by remember { mutableStateOf(AlarmSystem.alarmMinute(context)) }
    var refreshKey by remember { mutableStateOf(0) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshKey++
    }

    val steps = listOf("Bienvenue", "Réveil 03:00", "Autorisations", "Méthode")

    var page by remember { mutableStateOf(0) }

    Box(
        Modifier
            .fillMaxSize()
            .background(ContestColors.Background),
    ) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(46.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                steps.forEachIndexed { index, label ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= page) ContestColors.AccentRed else ContestColors.Border,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                if (page == 0) {
                    Text("ICT Computing Track", color = ContestColors.AccentRed, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("Ton cheat code de révision", color = ContestColors.OnSurface, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Chaque matin à l'heure que tu choisis, un réveil intrusif : il sonne tant que tu ne l'arrêtes pas. Ensuite, une session guidée qui enchaîne cours, QCM et examens blancs dans le format Huawei.",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(18.dp))
                    KeyCard(
                        title = "Ta mission quotidienne",
                        lines = listOf(
                            "1 chapitre par matière : openEuler, openGauss, Kunpeng",
                            "Un QCM de 20 questions en anglais à 60 % minimum",
                            "Chaque erreur expliquée en français",
                            "Mock exam quand une matière est complète",
                            "Examen de phase quand les 3 matières sont prêtes",
                        ),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("Phase de départ", color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("HCIA", "HCIP", "HCIE").forEach {
                            val selected = selectedPhase == it
                            Text(
                                it,
                                color = if (selected) Color.White else ContestColors.OnSurfaceMuted,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.large)
                                    .background(if (selected) ContestColors.AccentRed else ContestColors.Surface)
                                    .clickable {
                                        if (it == "HCIA" || selectedPhase != "HCIA") {}
                                        onSelectPhase(it)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Recommandé : commence par HCIA puis grimpe.",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 12.sp,
                    )
                }

                if (page == 1) {
                    Text("L'alarme du matin", color = ContestColors.OnSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Définit l'heure. Par défaut 03:00 comme demandé.",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(18.dp))
                    StepperRow("Heure", alarmHour, 0, 23) { alarmHour = it }
                    Spacer(Modifier.height(12.dp))
                    StepperRow("Minutes", alarmMinute, 0, 59) { alarmMinute = ((it / 5) * 5) }
                    Spacer(Modifier.height(18.dp))
                    KeyCard(
                        title = "Comportement du réveil",
                        lines = listOf(
                            "Son en boucle + vibration tant que tu n'arrêtes pas",
                            "Retour intrusif toutes les 90 s si tu le quittes",
                            "Calcul anti-sommeil + maintien 3,5 s pour arrêter",
                            "Une fois arrêté, la session s'ouvre automatiquement",
                        ),
                    )
                }

                if (page == 2) {
                    Text("Autorisations requises", color = ContestColors.OnSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sans elles, Android bloque les notifications et les réveils exacts.",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(18.dp))
                    PermissionBlock(
                        refreshKey = refreshKey,
                        onRequestNotif = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Astuce : arrive ici dans les étapes suivantes pour tout vérifier.",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 12.sp,
                    )
                }

                if (page == 3) {
                    Text("Méthode d'évaluation", color = ContestColors.OnSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Répartition officielle Computing Track utilisée pour les examens de phase :",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    SUBJECT_WEIGHTS.forEach { (subject, weight) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(MaterialTheme.shapes.large)
                                .background(ContestColors.Surface)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("●", color = subjectColor(subject))
                            Spacer(Modifier.width(10.dp))
                            Text(subject, color = ContestColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            Text("$weight %", color = ContestColors.OnSurfaceMuted, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    KeyCard(
                        title = "Seuils",
                        lines = listOf(
                            "QCM de chapitre/phase : 60 % minimum",
                            "Mock exam & examen de phase : 600/1000",
                            "Les examens contiennent des VRAI/FAUX, un choix, choix multiples et saisie libre",
                        ),
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (page > 0) {
                    Button(
                        onClick = { page-- },
                        modifier = Modifier.weight(0.6f),
                        colors = ButtonDefaults.buttonColors(containerColor = ContestColors.SurfaceHigh, contentColor = ContestColors.OnSurface),
                    ) { Text("Retour", color = ContestColors.OnSurfaceMuted) }
                } else {
                    Spacer(Modifier.weight(0.6f))
                }
                Button(
                    onClick = {
                        if (page == 3) {
                            AlarmSystem.setAlarmTime(context, alarmHour, alarmMinute)
                            onFinish()
                        } else page++
                    },
                    modifier = Modifier.weight(1.4f),
                    colors = ButtonDefaults.buttonColors(containerColor = ContestColors.AccentRed, contentColor = Color.White),
                ) {
                    Text(if (page == 3) "Tout est prêt" else "Suivant")
                }
            }
        }
    }
}

@Composable
private fun KeyCard(title: String, lines: List<String>) {
    CardBox {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = ContestColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            lines.forEach { line ->
                Text(
                    "• $line",
                    color = ContestColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
fun StepperRow(label: String, value: Int, min: Int, max: Int, onValue: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ContestColors.OnSurfaceMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("−", color = ContestColors.OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(ContestColors.SurfaceHigh)
            .clickable { if (value > min) onValue(value - 1) }
            .padding(horizontal = 16.dp, vertical = 6.dp))
        Spacer(Modifier.width(14.dp))
        Text("%02d".format(value), color = ContestColors.OnSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(14.dp))
        Text("+", color = ContestColors.OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(ContestColors.SurfaceHigh)
            .clickable { if (value < max) onValue(value + 1) }
            .padding(horizontal = 16.dp, vertical = 6.dp))
    }
}

@Composable
private fun PermissionBlock(refreshKey: Int, onRequestNotif: () -> Unit) {
    val context = LocalContext.current

    @Suppress("DEPRECATION")
    fun notifGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

    fun exactGranted(context: Context): Boolean =
        (context.getSystemService(AlarmManager::class.java))?.canScheduleExactAlarms() ?: true

    fun fsGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 34) {
            (context.getSystemService(NotificationManager::class.java))?.canUseFullScreenIntent() ?: true
        } else true

    fun batteryIgnored(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java)
        return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    fun Context.openManageOverlays() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        startActivity(intent)
    }

    fun Context.openBatteryPolicy() {
        val intent = Intent()
            .setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    fun Context.openExactAlarm() {
        val intent = Intent()
            .setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    fun Context.openFullScreenIntent() {
        val intent = Intent()
            .setAction(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            .setData(Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    PermissionRow("Notifications", notifGranted(context), onRequestNotif)
    PermissionRow("Alarmes exactes", exactGranted(context), { context.openExactAlarm() })
    PermissionRow("Affichage plein écran (verrouillé)", fsGranted(context), { context.openFullScreenIntent() })
    PermissionRow("Dessiner par-dessus les apps", Settings.canDrawOverlays(context), { context.openManageOverlays() })
    PermissionRow("Ignore l'économiseur de batterie", batteryIgnored(context), { context.openBatteryPolicy() })
    @Suppress("unused", "MemberVisibilityCanBePrivate")
    val keyRefresh = refreshKey
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onGrant: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.large)
            .background(ContestColors.Surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = ContestColors.OnSurface, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(14.dp))
        Text(
            if (granted) "OK ✓" else "Activer",
            color = if (granted) ContestColors.AccentGreen else Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier
                .padding(end = 14.dp)
                .clip(MaterialTheme.shapes.large)
                .background(if (granted) ContestColors.AccentGreen.copy(alpha = 0.15f) else ContestColors.AccentRed)
                .clickable { if (!granted) onGrant() }
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}
