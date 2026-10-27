package com.klein.ictcomputing.ui

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klein.ictcomputing.alarm.AlarmSystem
import com.klein.ictcomputing.data.SUBJECT_WEIGHTS

@Composable
fun SettingsScreen(onTestAlarm: () -> Unit) {
    val repo = rememberRepo()
    val context = androidx.compose.ui.platform.LocalContext.current
    var refreshKey by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }

    fun notifGranted(): Boolean = if (Build.VERSION.SDK_INT >= 33) {
        context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true

    fun exactGranted(): Boolean =
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() ?: true

    fun fsGranted(): Boolean = if (Build.VERSION.SDK_INT >= 34) {
        context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() ?: true
    } else true

    fun batteryIgnored(): Boolean {
        val pm = context.getSystemService(PowerManager::class.java)
        return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    fun dndGranted(): Boolean =
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted ?: false

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 56.dp),
    ) {
        Text(
            "Réglages",
            color = ContestColors.OnSurface,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        )

        Text("Alarme du matin", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        CardBox(Modifier.padding(horizontal = 18.dp)) {
            Column(Modifier.padding(16.dp)) {
                var hour by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(AlarmSystem.alarmHour(context)) }
                var minute by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(AlarmSystem.alarmMinute(context)) }

                StepperRow("Heure", hour, 0, 23) { hour = AlarmSystem.setAlarmTime(context, it, minute).let { _ -> hour; it } }
                Spacer(Modifier.height(10.dp))
                StepperRow("Minutes", minute, 0, 59) { minute = AlarmSystem.setAlarmTime(context, hour, it).let { _ -> minute; it } }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Réveil actif", color = ContestColors.OnSurface, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (AlarmSystem.enabled(context)) "OUI" else "NON",
                        color = if (AlarmSystem.enabled(context)) ContestColors.AccentGreen else ContestColors.AccentAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(ContestColors.SurfaceHigh)
                            .clickable { AlarmSystem.setEnabled(context, !AlarmSystem.enabled(context)) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Tester le réveil plein écran",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(ContestColors.AccentRed)
                        .clickable { onTestAlarm() }
                        .padding(vertical = 11.dp),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Autorisations", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        CardBox(Modifier.padding(horizontal = 18.dp)) {
            Column(Modifier.padding(10.dp)) {
                PermissionRow("Notifications", notifGranted()) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        (context as? android.app.Activity)?.let { act ->
                            androidx.core.app.ActivityCompat.requestPermissions(
                                act, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 41,
                            )
                        }
                    }
                    refreshKey++
                }
                PermissionRow("Alarmes exactes", exactGranted()) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                    )
                    refreshKey++
                }
                PermissionRow("Affichage plein écran", fsGranted()) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}")),
                    )
                    refreshKey++
                }
                PermissionRow("Dessiner par-dessus", Settings.canDrawOverlays(context)) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
                    )
                    refreshKey++
                }
                PermissionRow("Ne pas déranger (focus)", dndGranted()) {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    refreshKey++
                }
                PermissionRow("Ignorer l'économiseur", batteryIgnored()) {
                    context.startActivity(
                        Intent()
                            .setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:${context.packageName}")),
                    )
                    refreshKey++
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Phase active", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("HCIA", "HCIP", "HCIE").forEach { ph ->
                val selected = repo.progress.selectedPhase == ph
                Text(
                    ph,
                    color = if (selected) Color.White else ContestColors.OnSurfaceMuted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(if (selected) phaseColor(ph) else ContestColors.Surface)
                        .clickable { repo.selectPhase(ph) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Pondération officielle Computing Track", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        SUBJECT_WEIGHTS.forEach { (subject, weight) ->
            Row(
                Modifier
                    .padding(horizontal = 18.dp)
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(ContestColors.Surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("●", color = subjectColor(subject))
                Spacer(Modifier.width(10.dp))
                Text(subject, color = ContestColors.OnSurface, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text("$weight %", color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("Contenu", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        val stats = repo.contentStats()
        CardBox(Modifier.padding(horizontal = 18.dp)) {
            Column(Modifier.padding(14.dp)) {
                MiniStatLine("Chapitres de cours", "${stats.first}")
                MiniStatLine("Questions de la banque", "${stats.second}")
                MiniStatLine("Minutes de cours", "${stats.third}")
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Remise à zéro",
            color = ContestColors.OnSurfaceMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Effacer ma progression",
            color = ContestColors.AccentRed,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(ContestColors.AccentRed.copy(alpha = 0.12f))
                .clickable { repo.resetProgress() }
                .padding(vertical = 12.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "ICT Computing Track v1.0 — préparation Huawei ICT Competition.\nSeuils officiels: QCM 60 %, mocks et phase 600/1000. Pondération openEuler 50 %, openGauss 30 %, Kunpeng 20 %.\nSources : supports HCIA/HCIP/HCIE openEuler & openGauss, HCIA-Kunpeng, outline 2025-2026.",
            color = ContestColors.OnSurfaceMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun MiniStatLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
        Text(label, color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = ContestColors.OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
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
