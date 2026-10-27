package com.klein.ictcomputing.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.klein.ictcomputing.data.Chapter
import com.klein.ictcomputing.data.CourseSection
import com.klein.ictcomputing.focus.FocusMode

/**
 * Cours « vivant » : sommaire cliquable, boîtes visuelles (objectifs / à retenir / pièges),
 * code mono copiable, source PDF mise en valeur. La sémantique reste identique.
 */
@Composable
fun CourseScreen(
    chapterId: String,
    onBack: () -> Unit,
    onTakeQuiz: (String) -> Unit,
) {
    val repo = rememberRepo()
    val chapter = repo.chapterById(chapterId)
    if (chapter == null) {
        TopBar("Chapitre indisponible", null, onBack)
        return
    }
    val context = LocalContext.current
    LaunchedEffect(chapterId) {
        repo.markRead(chapterId)
        // Mode concentration : un chapitre non validé ne peut pas être quitté.
        if (!repo.chapterProgress(chapterId).passed) {
            (context as? android.app.Activity)?.let { FocusMode.begin(it, chapterId) }
        }
    }
    val locked = FocusMode.active
    val color = subjectColor(chapter.subject)
    val scope = rememberCoroutineScope()

    // ordre des blocs : 0 = en-tête, 1 = sommaire, 2..2+n-1 = sections, ... ;
    // le sommaire scrolle vers (2 + sectionIndex)
    val listState = rememberLazyListState()

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Ch. ${chapter.number} · ${chapter.titleFr}", null, onBack, locked = locked)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 150.dp),
        ) {
            // ---------- En-tête ----------
            item(key = "head") {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChipPill(chapter.phase + " · " + chapter.subject, color)
                        Spacer(Modifier.width(8.dp))
                        ChipPill("${chapter.minutes} min", ContestColors.OnSurfaceMuted)
                        Spacer(Modifier.width(8.dp))
                        if (repo.chapterProgress(chapter.id).passed) ChipPill("validé ✓", ContestColors.AccentGreen)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(chapter.titleEn, color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ---------- Sommaire cliquable ----------
            item(key = "toc") {
                if (chapter.sections.size > 1) {
                    Column(Modifier.padding(horizontal = 18.dp)) {
                        Text(
                            "SOMMAIRE — sauter à une partie",
                            color = ContestColors.OnSurfaceMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            chapter.sections.forEachIndexed { si, s ->
                                Box(
                                    Modifier
                                        .padding(end = 8.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(color.copy(alpha = 0.12f))
                                        .clickable { scope.launch { listState.scrollToItem(index = si + 2) } }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Text("${si + 1}", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // ---------- Objectifs ----------
            item(key = "obj") {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    InfoBox(
                        title = "🎯 Objectifs",
                        accent = color,
                        background = ContestColors.Surface,
                    ) {
                        chapter.objectives.forEach { obj ->
                            Text("▸ $obj", color = ContestColors.OnSurfaceMuted, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            // ---------- Sections ----------
            chapter.sections.forEachIndexed { si, section ->
                item(key = "sec-$si") {
                    Column(Modifier.padding(horizontal = 18.dp)) {
                        SectionHeaderBar(number = si + 1, accent = color, title = section.heading)
                        Spacer(Modifier.height(8.dp))
                        section.body.split("\n\n").forEach { paragraph ->
                            if (paragraph.isNotBlank()) {
                                Text(
                                    paragraph.trim(),
                                    color = ContestColors.OnSurface.copy(alpha = 0.92f),
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.padding(vertical = 3.dp),
                                )
                            }
                        }
                        section.bullets.forEach { bullet ->
                            Text(
                                "• $bullet",
                                color = ContestColors.OnSurface.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                        section.diagram?.let { diagram ->
                            Spacer(Modifier.height(10.dp))
                            DiagramBlock(diagram, accent = color)
                        }
                        if (section.code.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            RichCodeBlock(section.code, accent = color)
                        }
                        section.table?.let { table ->
                            Spacer(Modifier.height(10.dp))
                            DataTable(table.headers, table.rows)
                        }
                        section.exercises.forEachIndexed { ei, exercise ->
                            Spacer(Modifier.height(10.dp))
                            ExerciseCard(
                                number = ei + 1,
                                prompt = exercise.prompt,
                                correction = exercise.correction,
                                accent = color,
                            )
                        }
                        section.tip?.let { tip ->
                            Spacer(Modifier.height(10.dp))
                            CardBox(background = ContestColors.AccentAmber.copy(alpha = 0.08f), corner = 16.dp) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                    Text("⚠", color = ContestColors.AccentAmber, fontSize = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(tip, color = ContestColors.AccentAmber.copy(alpha = 0.95f), fontSize = 13.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }

            // ---------- À retenir ----------
            if (chapter.keyPoints.isNotEmpty()) {
                item(key = "kp") {
                    Column(Modifier.padding(horizontal = 18.dp)) {
                        InfoBox(
                            title = "💡 À retenir",
                            accent = ContestColors.AccentGreen,
                            background = ContestColors.AccentGreen.copy(alpha = 0.08f),
                            corner = 18.dp,
                        ) {
                            chapter.keyPoints.forEach { kp ->
                                Text("✔ $kp", color = ContestColors.OnSurface, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(vertical = 3.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // ---------- Pièges ----------
            if (chapter.traps.isNotEmpty()) {
                item(key = "traps") {
                    Column(Modifier.padding(horizontal = 18.dp)) {
                        InfoBox(
                            title = "⚡ pièges d'examen",
                            accent = ContestColors.AccentRed,
                            background = ContestColors.AccentRed.copy(alpha = 0.08f),
                            corner = 18.dp,
                        ) {
                            chapter.traps.forEach { trap ->
                                Text("✗ $trap", color = ContestColors.OnSurface.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // ---------- Source PDF officiel ----------
            item(key = "src") {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    CardBox(background = ContestColors.SurfaceHigh, corner = 14.dp) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Text("📚", fontSize = 13.sp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Ressource officielle",
                                    color = ContestColors.OnSurfaceMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    chapter.source,
                                    color = ContestColors.OnSurface.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }

        // ---------- Barre basse ----------
        Box(Modifier.align(Alignment.CenterHorizontally)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(ContestColors.SurfaceLow.copy(alpha = 0.98f))
                    .padding(14.dp),
            ) {
                Text(
                    "Passer le test du chapitre",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(ContestColors.AccentRed)
                        .clickable { onTakeQuiz(chapter.id) }
                        .padding(vertical = 13.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${chapter.quiz.size} questions · anglais, explications FR · seuil 60 %",
                    color = ContestColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

/* -------- composants spécifiques cours livré -------- */

@Composable
private fun InfoBox(
    title: String,
    accent: Color,
    background: Color,
    corner: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    CardBox(background = background, corner = corner) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(Modifier.padding(14.dp)) {
                Text(title, color = accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun SectionHeaderBar(number: Int, accent: Color, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
        ) {
            Text(
                "$number",
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(title, color = ContestColors.OnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Schéma ASCII lisible sur mobile, avec défilement horizontal si nécessaire. */
@Composable
private fun DiagramBlock(diagram: String, accent: Color) {
    CardBox(background = ContestColors.SurfaceLow, corner = 14.dp) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "🧭 Schéma",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                diagram.trim('\n'),
                color = ContestColors.OnSurface.copy(alpha = 0.9f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
    }
}

/** Exercice guidé : énoncé puis corrigé détaillé. */
@Composable
private fun ExerciseCard(number: Int, prompt: String, correction: String, accent: Color) {
    CardBox(background = accent.copy(alpha = 0.07f), corner = 16.dp) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "🧪 EXERCICE CORRIGÉ $number",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                prompt,
                color = ContestColors.OnSurface,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(9.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ContestColors.AccentGreen.copy(alpha = 0.09f))
                    .padding(11.dp),
            ) {
                Column {
                    Text(
                        "✔ Corrigé",
                        color = ContestColors.AccentGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        correction,
                        color = ContestColors.OnSurface.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                }
            }
        }
    }
}

/** Bloc de code mono avec copie en un clic — rendu plus vivant. */
@Composable
private fun RichCodeBlock(lines: List<String>, accent: Color = ContestColors.AccentCyan) {
    val ctx = LocalContext.current
    CardBox(background = ContestColors.SurfaceLow, corner = 14.dp) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💻 Terminal", color = ContestColors.OnSurfaceMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    "📋 copier",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("cmd", lines.joinToString("\n")))
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            lines.filter { it.isNotBlank() }.forEach { line ->
                Text(
                    line,
                    color = ContestColors.AccentGreen.copy(alpha = 0.92f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(vertical = 1.dp),
                )
            }
        }
    }
}
