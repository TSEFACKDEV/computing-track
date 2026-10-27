package com.klein.ictcomputing.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klein.ictcomputing.data.AppRepository
import com.klein.ictcomputing.data.levelOf
import com.klein.ictcomputing.data.xpInLevel
import com.klein.ictcomputing.data.DailyTask
import com.klein.ictcomputing.data.TaskKind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenTask: (DailyTask?, String) -> Unit,
    onOpenSubject: (String, String) -> Unit,
    onTestAlarm: () -> Unit,
    onOpenCoach: () -> Unit = {},
) {
    val repo = rememberRepo()
    val phaseId = repo.progress.selectedPhase
    val phase = repo.phase(phaseId)
    val tasks = repo.dailyTasks()
    val nextAlarm = repo.nextAlarmMillis()
    val dueCoach = repo.coachCount()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 56.dp),
    ) {
        HeroCard(
            phase = phase?.title ?: phaseId,
            subtitle = phase?.subtitle ?: "",
            streak = repo.progress.streak,
        )

        // -------- Coach Mémoire : carte de rattrapage --------
        if (dueCoach > 0) {
            Row(
                Modifier
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(ContestColors.AccentAmber.copy(alpha = 0.16f))
                    .clickable { onOpenCoach() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🧠", fontSize = 22.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Coach · $dueCoach notion(s) à rattraper",
                        color = ContestColors.AccentAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Reviens jusqu'à maîtriser tes notions ratées ✓",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 12.sp,
                    )
                }
                Text("›", color = ContestColors.AccentAmber, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                Modifier
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(ContestColors.Surface)
                    .clickable { onOpenCoach() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🧠", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Coach · Mémoire", color = ContestColors.OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (repo.progress.clears.isNotEmpty()) "${repo.progress.clears.size} notion(s) maîtrisée(s) 🏅"
                        else "Aucune notion en dette. Un raté en quiz la fait rentrer ici.",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 12.sp,
                    )
                }
                Text("›", color = ContestColors.OnSurfaceMuted, fontSize = 22.sp)
            }
        }

        val xp = repo.progress.xp
        Row(
            Modifier
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(ContestColors.Surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Niveau ${levelOf(xp)}",
                        color = ContestColors.AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${xpInLevel(xp)}/500 XP",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(5.dp))
                Meter(xpInLevel(xp) / 500f, color = ContestColors.AccentCyan, height = 5.dp)
            }
        }

        val todayHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val allDoneToday = tasks.all { it.done }
        if (!allDoneToday && todayHour >= 19) {
            Row(
                Modifier
                    .padding(horizontal = 18.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(ContestColors.AccentRed.copy(alpha = 0.14f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔥", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Ta série de ${repo.progress.streak} j expire à minuit : termine tes 3 sujets !",
                    color = ContestColors.AccentRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                )
            }
        }

        val tf = SimpleDateFormat("EEE d MMM • HH:mm", Locale.FRANCE)
        val alarmLabel = if (nextAlarm > 0) tf.format(Date(nextAlarm)) else "Pas programmé"
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(MaterialTheme.shapes.large)
                .background(ContestColors.Surface)
                .clickable { onTestAlarm() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Réveil programmé", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
                Text(alarmLabel, color = ContestColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("Tester", color = ContestColors.AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))
        Text("SESSION DU JOUR", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp))
        Spacer(Modifier.height(8.dp))
        tasks.forEachIndexed { index, task ->
            TaskRow(task = task, phaseId = phaseId) { onOpenTask(task, phaseId) }
            if (index != tasks.lastIndex) Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(22.dp))
        Text("PROGRÈS DE PHASE", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp))
        Spacer(Modifier.height(8.dp))
        repo.subjects(phaseId).forEach { subject ->
            val sp = repo.subjectProgress(phaseId, subject.subject)
            SubjectRow(subject.subject, sp) { onOpenSubject(phaseId, subject.subject) }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(18.dp))
        val ready = repo.phaseReady(phaseId)
        val latest = repo.phaseExams(phaseId).maxByOrNull { it.date }
        CardBox(Modifier.padding(horizontal = 18.dp), background = ContestColors.Surface) {
            Column(Modifier.padding(16.dp)) {
                Text("Examen de phase $phaseId", color = ContestColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (ready) "Prêt ! L'examinateur t'attend." else "Débloqué quand les 3 matières (chapitres + mock) sont validées.",
                    color = if (ready) ContestColors.AccentGreen else ContestColors.OnSurfaceMuted,
                    fontSize = 12.sp,
                )
                latest?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "meilleur score: ${it.score}/1000 ${if (it.passed) "✓" else ""}",
                        color = if (it.passed) ContestColors.AccentGreen else ContestColors.AccentAmber,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(phase: String, subtitle: String, streak: Int) {
    Box(
        Modifier
            .padding(18.dp, 18.dp, 18.dp, 6.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    listOf(ContestColors.AccentRedDeep, ContestColors.AccentRed.copy(alpha = 0.72f)),
                ),
            )
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(phase, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("série", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                    Text("$streak j", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = Color(0xE6FFFFFF), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TaskRow(task: DailyTask, phaseId: String, onClick: () -> Unit) {
    val done = task.done
    val color = subjectColor(task.subject)
    CardBox(Modifier.padding(horizontal = 18.dp), background = if (done) ContestColors.SurfaceLow else ContestColors.Surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(task.subject, color = ContestColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    when (task.kind) {
                        TaskKind.CHAPTER -> task.chapter?.let { meta -> "Chapitre ${meta.title}" } ?: "Chapitre"
                        TaskKind.MOCK -> "Mock exam (matière complétée)"
                        TaskKind.PHASE_EXAM -> "Examen de phase"
                        TaskKind.DONE -> "Module terminé"
                    },
                    color = ContestColors.OnSurfaceMuted,
                    fontSize = 12.sp,
                )
            }
            ChipPill(
                text = when {
                    done -> "Fait"
                    task.kind == TaskKind.CHAPTER -> task.chapter?.minutes?.let { "$it min" } ?: "Cours"
                    else -> "À faire"
                },
                color = when {
                    done -> ContestColors.AccentGreen
                    task.kind == TaskKind.CHAPTER -> color
                    else -> ContestColors.AccentAmber
                },
            )
        }
    }
}

@Composable
private fun SubjectRow(subject: String, sp: com.klein.ictcomputing.data.SubjectProgress, onClick: () -> Unit) {
    CardBox(Modifier.padding(horizontal = 18.dp), background = ContestColors.Surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(subject, color = ContestColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Meter(
                    value = if (sp.totalChapters > 0) sp.passedChapters.toFloat() / sp.totalChapters else 0f,
                    color = subjectColor(subject),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${sp.passedChapters}/${sp.totalChapters} chapitres" + if (sp.mockPassed) " • mock ✓" else "",
                    color = ContestColors.OnSurfaceMuted,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text("→", color = ContestColors.OnSurfaceMuted, fontSize = 20.sp)
        }
    }
}

@Composable
internal fun rememberRepo(): AppRepository {
    val app = LocalApp.current ?: error("AppRepository introuvable")
    return app.repo
}
