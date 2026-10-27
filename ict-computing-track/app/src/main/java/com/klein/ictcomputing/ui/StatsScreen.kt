package com.klein.ictcomputing.ui

import com.klein.ictcomputing.data.levelOf
import com.klein.ictcomputing.data.xpInLevel

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.klein.ictcomputing.data.SUBJECTS

@Composable
fun StatsScreen(onOpenBoss: (String) -> Unit = {}) {
    val repo = rememberRepo()
    val progress = repo.progress

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 64.dp),
    ) {
        Text(
            "Progrès",
            color = ContestColors.OnSurface,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        )

        // -------- Arène Boss : trois boss à affronter --------
        Column(Modifier.padding(horizontal = 18.dp)) {
            Text("⚖️ Arène Boss", color = ContestColors.OnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            SUBJECTS.forEach { subject ->
                val key = "${progress.selectedPhase}|$subject"
                val boss = repo.progress.bosses[key]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(ContestColors.Surface)
                        .clickable { onOpenBoss(subject) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        subject,
                        color = subjectColor(subject),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        when {
                            boss == null -> "Non rencontré"
                            boss.defeated -> "Vaincu ${boss.wins}× 🏆"
                            else -> "Meilleur essai ${boss.lastCorrect}/12"
                        },
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("⚔️", fontSize = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(Modifier.padding(horizontal = 18.dp)) {
            globalCard(
                title = "Vision d'ensemble",
                lines = listOf(
                    "Série active: ${progress.streak} jours",
                    "Niveau ${levelOf(progress.xp)} · ${progress.xp} XP total",
                    "Questions traitées: ${progress.totalQuestions}",
                    "Réponses justes: ${progress.totalCorrect}",
                    "Précision: ${if (progress.totalQuestions > 0) (progress.totalCorrect * 100) / progress.totalQuestions else 0} %",
                ),
            )
        }
        Spacer(Modifier.height(16.dp))

        listOf("HCIA", "HCIP", "HCIE").forEach { phase ->
            val subs = repo.subjects(phase)
            if (subs.isEmpty()) return@forEach
            Column(Modifier.padding(horizontal = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("●", color = phaseColor(phase))
                    Spacer(Modifier.width(8.dp))
                    Text("Phase $phase", color = ContestColors.OnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                subs.forEach { subject ->
                    val sp = repo.subjectProgress(phase, subject.subject)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(ContestColors.Surface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("●", color = subjectColor(subject.subject))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(subject.subject, color = ContestColors.OnSurface, fontSize = 14.sp)
                        }
                        Text(
                            "${sp.passedChapters}/${sp.totalChapters}" + if (sp.mockPassed) " • mock ✓" else "",
                            color = ContestColors.OnSurfaceMuted,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                // Meilleurs mock exams
                subs.forEach { subject ->
                    val mock = progress.mocks["$phase|${subject.subject}"] ?: return@forEach
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("mock ${subject.subject}", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${mock.score}/1000", color = if (mock.passed) ContestColors.AccentGreen else ContestColors.AccentAmber, fontSize = 12.sp)
                    }
                }
                // Past phase exams
                val exams = repo.phaseExams(phase)
                exams.sortedByDescending { it.date }.take(4).forEach { pe ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("examen $phase", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${pe.date} · ${pe.score}/1000" + if (pe.passed) " ✓" else "",
                            color = if (pe.passed) ContestColors.AccentGreen else ContestColors.AccentAmber,
                            fontSize = 12.sp,
                        )
                    }
                }
                if (exams.isEmpty()) {
                    Text(
                        "Aucun examen de phase encore",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun globalCard(title: String, lines: List<String>) {
    CardBox(background = ContestColors.Surface) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = ContestColors.OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
            lines.forEach { line ->
                Text(line, color = ContestColors.OnSurfaceMuted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}
