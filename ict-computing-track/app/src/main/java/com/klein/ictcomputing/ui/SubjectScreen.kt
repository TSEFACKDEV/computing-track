package com.klein.ictcomputing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klein.ictcomputing.data.BuiltExam

@Composable
fun SubjectScreen(
    phaseId: String,
    subjectId: String,
    onBack: () -> Unit,
    onOpenChapter: (String) -> Unit,
    onOpenMock: (BuiltExam?) -> Unit,
) {
    val repo = rememberRepo()
    val subject = repo.subject(phaseId, subjectId) ?: run {
        TopBar(title = subjectId, onBack = onBack)
        return
    }
    val color = subjectColor(subject.subject)
    val sp = repo.subjectProgress(phaseId, subject.subject)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 56.dp),
    ) {
        TopBar(title = subject.subject, subtitle = subject.tagline, onBack = onBack)

        Column(Modifier.padding(horizontal = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("●", color = color)
                Spacer(Modifier.width(8.dp))
                Text("$phaseId · poids ${subject.weight} %", color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("${sp.passedChapters}/${sp.totalChapters}", color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(10.dp))
            Meter(sp.percent / 100f, color = color)
        }

        Spacer(Modifier.height(16.dp))
        Text("Chapitres", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp))
        Spacer(Modifier.height(8.dp))

        subject.chapters.forEachIndexed { index, meta ->
            val cp = repo.chapterProgress(meta.id)
            val unlocked = index == 0 || repo.chapterProgress(subject.chapters[index - 1].id).passed
            val done = cp.passed
            ChapterRow(
                index = index + 1,
                title = meta.title,
                minutes = meta.minutes,
                unlocked = unlocked,
                done = done,
                best = cp.bestScore,
                color = color,
            ) {
                if (unlocked) onOpenChapter(meta.id)
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))
        val mockReady = sp.passedChapters == sp.totalChapters && sp.totalChapters > 0
        Row(
            Modifier
                .padding(horizontal = 18.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(if (mockReady) color.copy(alpha = 0.18f) else ContestColors.SurfaceLow)
                .clickable {
                    if (mockReady) {
                        onOpenMock(repo.buildSubjectMockExam(phaseId, subject.subject))
                    }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Mock exam • ${subject.subject}", color = ContestColors.OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (mockReady) "Toutes les notions du référentiel sont couvertes : lance ton blanc." else "Termine tous les chapitres pour le débloquer.",
                    color = ContestColors.OnSurfaceMuted,
                    fontSize = 12.sp,
                )
            }
            ChipPill(if (mockReady) "Repartir" else "Verrouillé", if (mockReady) ContestColors.AccentGreen else ContestColors.OnSurfaceMuted)
        }
        if (sp.mockPassed) {
            val mock = repo.progress.mocks["$phaseId|${subject.subject}"]
            Spacer(Modifier.height(6.dp))
            Text(
                "Meilleur mock : ${mock?.score ?: 0}/1000",
                color = ContestColors.AccentGreen,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ChapterRow(
    index: Int,
    title: String,
    minutes: Int,
    unlocked: Boolean,
    done: Boolean,
    best: Int,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    CardBox(Modifier.padding(horizontal = 18.dp), background = if (done) ContestColors.SurfaceLow else ContestColors.Surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(if (done) color.copy(alpha = 0.2f) else ContestColors.SurfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$index",
                    color = if (done) color else ContestColors.OnSurfaceMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ContestColors.OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        done && best > 0 -> "Validé • meilleur score $best %"
                        unlocked -> "Disponible • $minutes min de cours + QCM"
                        else -> "Verrouillé : finis le chapitre précédent"
                    },
                    color = ContestColors.OnSurfaceMuted,
                    fontSize = 12.sp,
                )
            }
            ChipPill(
                when {
                    done -> "✓"
                    unlocked -> "Commencer"
                    else -> "🔒".replace("🔒", "•")
                },
                when {
                    done -> ContestColors.AccentGreen
                    unlocked -> color
                    else -> ContestColors.OnSurfaceMuted
                },
            )
        }
    }
}

@Composable
fun TopBar(title: String, subtitle: String? = null, onBack: () -> Unit, locked: Boolean = false) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (locked) "🔒" else "←",
                color = if (locked) ContestColors.AccentAmber else ContestColors.OnSurface,
                fontSize = if (locked) 15.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(ContestColors.SurfaceHigh)
                    .clickable(enabled = !locked) { onBack() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(title, color = ContestColors.OnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = ContestColors.OnSurfaceMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 52.dp))
        }
    }
}
