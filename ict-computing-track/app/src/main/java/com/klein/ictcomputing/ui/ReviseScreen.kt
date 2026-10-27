package com.klein.ictcomputing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
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
import com.klein.ictcomputing.data.BuiltExam

@Composable
fun ReviseScreen(
    onOpenSubject: (String, String) -> Unit,
    onOpenPhaseExam: (BuiltExam?) -> Unit,
) {
    val repo = rememberRepo()
    val phaseId = repo.progress.selectedPhase
    val phase = repo.phase(phaseId)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 56.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Programme $phaseId", color = ContestColors.OnSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                phase?.description ?: "Chapitres du référentiel Computing Track",
                color = ContestColors.OnSurfaceMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("HCIA", "HCIP", "HCIE").forEach { id ->
                    val selected = phaseId == id
                    Text(
                        id,
                        color = if (selected) Color.White else ContestColors.OnSurfaceMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(if (selected) phaseColor(id) else ContestColors.Surface)
                            .clickable { repo.selectPhase(id) }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        repo.subjects(phaseId).forEach { subject ->
            val sp = repo.subjectProgress(phaseId, subject.subject)
            MockRow(subject.subject, subject.tagline, sp) { onOpenSubject(phaseId, subject.subject) }
            Spacer(Modifier.height(10.dp))
        }
        if (repo.subjects(phaseId).isEmpty()) {
            CardBox(Modifier.padding(horizontal = 18.dp), background = ContestColors.Surface) {
                Text(
                    "Contenu de cette phase en cours de rédaction : reviens bientôt.",
                    color = ContestColors.OnSurfaceMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        val ready = repo.phaseReady(phaseId)
        Row(
            Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(if (ready) phaseColor(phaseId).copy(alpha = 0.2f) else ContestColors.SurfaceLow)
                .clickable {
                    if (ready) {
                        val exam = repo.buildPhaseExam(phaseId)
                        if (exam != null) onOpenPhaseExam(exam)
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Examen de phase Computing Track", color = ContestColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (ready) "Détoquons : ${repo.phase(phaseId)?.durationMin ?: 60} min, pondération 50/30/20." else "Chapitres + mock exams requis.",
                    color = ContestColors.OnSurfaceMuted,
                    fontSize = 12.sp,
                )
            }
            ChipPill(if (ready) "Commencer" else "Verrouillé", if (ready) ContestColors.AccentGreen else ContestColors.OnSurfaceMuted)
        }
    }
}

@Composable
private fun MockRow(subject: String, tagline: String, sp: com.klein.ictcomputing.data.SubjectProgress, onClick: () -> Unit) {
    val color = subjectColor(subject)
    CardBox(Modifier.padding(horizontal = 18.dp), background = ContestColors.Surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("●", color = color)
                    Spacer(Modifier.width(8.dp))
                    Text(subject, color = ContestColors.OnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("${sp.percent} %", color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (tagline.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(tagline, color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Meter(sp.percent / 100f, color = color)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${sp.passedChapters}/${sp.totalChapters} chapitres" + if (sp.mockPassed) " • mock ✓" else " • mock à venir",
                    color = ContestColors.OnSurfaceMuted,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text("→", color = ContestColors.OnSurfaceMuted, fontSize = 20.sp)
        }
    }
}
