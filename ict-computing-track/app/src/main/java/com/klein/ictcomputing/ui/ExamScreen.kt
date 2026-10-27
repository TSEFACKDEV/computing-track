package com.klein.ictcomputing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klein.ictcomputing.data.BuiltExam
import com.klein.ictcomputing.data.textMatches
import com.klein.ictcomputing.data.ExamQuestion
import com.klein.ictcomputing.data.QType
import kotlinx.coroutines.delay

@Composable
fun ExamScreen(
    exam: BuiltExam,
    onBack: () -> Unit,
) {
    val repo = rememberRepo()
    val accent = if (exam.subject != null) subjectColor(exam.subject) else ContestColors.AccentRed

    var started by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var index by remember { mutableStateOf(0) }
    var secondsLeft by remember { mutableStateOf(exam.durationMin * 60) }
    var showPalette by remember { mutableStateOf(true) }

    var csingle by remember { mutableStateOf(mapOf<Int, List<Int>>()) }
    var ctext by remember { mutableStateOf(mapOf<Int, String>()) }

    fun gradeEq(q: ExamQuestion, pick: List<Int>, text: String): Boolean = when (q.question.type) {
        QType.TEXT -> textMatches(text, q.question.accept)
        else -> pick.toSet() == q.question.answer.toSet()
    }

    fun correctCount(): Int = exam.questions.indices.count { i ->
        val q = exam.questions[i]
        gradeEq(q, csingle[i] ?: emptyList(), ctext[i] ?: "")
    }

    fun submit() {
        if (submitted) return
        submitted = true
        val correct = correctCount()
        val total = exam.questions.size
        val score = (correct * 1000) / total.coerceAtLeast(1)
        val passed = score >= 600
        if (exam.subject != null) {
            repo.recordMock(exam.phase, exam.subject, correct, total, score)
        } else {
            repo.recordPhaseExam(exam.phase, correct, total, score)
        }
    }

    BackHandler(enabled = started && !submitted) {
        submit()
    }

    LaunchedEffect(started, submitted) {
        if (started && !submitted) {
            while (secondsLeft > 0 && !submitted) {
                delay(1000)
                secondsLeft -= 1
                if (secondsLeft <= 0) submit()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(ContestColors.Background)) {
        TopBar(title = exam.title, subtitle = exam.subtitle, onBack = onBack)
        if (!started) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CardBox(Modifier.fillMaxWidth(), background = ContestColors.Surface) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Format type Huawei", color = ContestColors.OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            buildString {
                                append("- ${exam.questions.size} questions (${exam.durationMin} min)\n")
                                append("- VRAI/FAUX, un choix, choix multiples, saisie libre\n")
                                append("- Aucun feedback pendant l'épreuve\n")
                                append("- Score /1000, seuil 600\n")
                                if (exam.subject == null) append("- Pondération officielle 50/30/20\n")
                            },
                            color = ContestColors.OnSurfaceMuted,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Commencer",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(accent)
                        .clickable { started = true }
                        .padding(vertical = 14.dp),
                )
            }
        } else if (!submitted) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Question ${index + 1}/${exam.questions.size}",
                    color = ContestColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.weight(1f))
                val mins = secondsLeft / 60
                val secs = secondsLeft % 60
                Text(
                    "%02d:%02d".format(mins, secs),
                    color = if (secondsLeft < 120) ContestColors.AccentRed else ContestColors.AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Meter(secondsLeft / (exam.durationMin * 60f), color = accent, height = 3.dp)
            Spacer(Modifier.height(8.dp))

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 140.dp),
            ) {
                val q = exam.questions[index]
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChipPill("Q${index + 1}", accent)
                        Spacer(Modifier.width(8.dp))
                        ChipPill(q.subject, subjectColor(q.subject))
                        Spacer(Modifier.weight(1f))
                        ChipPill(if (q.question.type == QType.MULTI) "multiple" else if (q.question.type == QType.TF) "V/F" else if (q.question.type == QType.TEXT) "Libre" else "Unique", ContestColors.OnSurfaceMuted)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(q.question.prompt, color = ContestColors.OnSurface, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))

                    if (q.question.type == QType.TEXT) {
                        OutlinedTextField(
                            value = ctext[index] ?: "",
                            onValueChange = { v -> ctext = ctext + (index to v) },
                            label = { Text("Ta réponse") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            keyboardActions = KeyboardActions(onDone = { }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        q.question.options.forEachIndexed { oi, option ->
                            val picked = (csingle[index] ?: emptyList()).contains(oi)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(if (picked) accent.copy(alpha = 0.16f) else ContestColors.Surface)
                                    .clickable {
                                        val current = csingle[index] ?: emptyList()
                                        val nextPick = when (q.question.type) {
                                            QType.MULTI -> if (picked) current - oi else current + oi
                                            else -> listOf(oi)
                                        }
                                        csingle = csingle + (index to nextPick)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "ABCD".takeIf { oi < 4 }?.get(oi)?.toString() ?: (oi + 1).toString(),
                                    color = ContestColors.OnSurfaceMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(22.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(option, color = ContestColors.OnSurface, fontSize = 14.sp, lineHeight = 19.sp)
                            }
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(ContestColors.SurfaceLow.copy(alpha = 0.98f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Palette",
                    color = ContestColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(ContestColors.SurfaceHigh)
                        .clickable { showPalette = !showPalette }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Terminer et corriger",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(ContestColors.AccentRed)
                        .clickable { submit() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
            if (showPalette) {
                PaletteStrip(
                    totalCount = exam.questions.size,
                    answered = { i ->
                        if (exam.questions[i].question.type == QType.TEXT) (ctext[i] ?: "").isNotBlank() else (csingle[i] ?: emptyList()).isNotEmpty()
                    },
                    currentIndex = index,
                    accent = accent,
                ) { clicked ->
                    index = clicked
                    showPalette = false
                }
            }
        } else {
            // Résultat
            val correct = correctCount()
            val total = exam.questions.size
            val score = (correct * 1000) / total.coerceAtLeast(1)
            val passed = score >= 600

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 140.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (passed) "RÉUSSI" else "INSUFFISANT",
                            color = if (passed) ContestColors.AccentGreen else ContestColors.AccentRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text("$score/1000", color = if (passed) ContestColors.AccentGreen else ContestColors.AccentRed, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Meter(score / 1000f, color = if (passed) ContestColors.AccentGreen else ContestColors.AccentRed)
                    Spacer(Modifier.height(12.dp))
                    Text("Répartition correcte : $correct / $total — seuil 600", color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)

                    Spacer(Modifier.height(16.dp))

                    // Détail par matière
                    val perSubject = exam.questions.indices.groupBy { exam.questions[it].subject }
                    Column {
                        perSubject.forEach { (subject, indices) ->
                            val c = indices.count { i -> gradeEq(exam.questions[i], csingle[i] ?: emptyList(), ctext[i] ?: "") }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("●", color = subjectColor(subject))
                                Spacer(Modifier.width(8.dp))
                                Text(subject, color = ContestColors.OnSurface, fontSize = 13.sp)
                                Spacer(Modifier.weight(1f))
                                Text("$c/${indices.size}", color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)
                            }
                            Meter(c.toFloat() / indices.size.coerceAtLeast(1), color = subjectColor(subject), height = 4.dp)
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Retour au programme",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(ContestColors.AccentRed)
                            .clickable { onBack() }
                            .padding(vertical = 12.dp),
                    )

                    val wrongOnes = exam.questions.indices.filter { i ->
                        val q = exam.questions[i]
                        !gradeEq(q, csingle[i] ?: emptyList(), ctext[i] ?: "")
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Correction (${wrongOnes.size} ratée(s))", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    wrongOnes.forEach { i ->
                        val q = exam.questions[i]
                        CardBox(background = ContestColors.AccentRed.copy(alpha = 0.07f)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ChipPill(q.subject, subjectColor(q.subject))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Q${i + 1}", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(q.question.prompt, color = ContestColors.OnSurface, fontSize = 13.sp, lineHeight = 19.sp)
                                if (q.question.type == QType.TEXT) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("Ta réponse : ${ctext[i] ?: "(vide)"}", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
                                } else {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Ta sélection : " + (csingle[i] ?: emptyList()).takeIf { it.isNotEmpty() }
                                            ?.joinToString(", ") { oi -> "ABCD".getOrNull(oi)?.toString() ?: "?" }
                                            ?: "(aucune)",
                                        color = ContestColors.OnSurfaceMuted,
                                        fontSize = 12.sp,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Réponse : " + when (q.question.type) {
                                        QType.TEXT -> q.question.sampleAnswer ?: q.question.accept.firstOrNull() ?: ""
                                        else -> q.question.answer.joinToString(" • ") { oi ->
                                            q.question.options.getOrNull(oi)?.let { "ABCD".getOrNull(oi)?.toString() + " " + it } ?: "ABCD".getOrNull(oi)?.toString() ?: ""
                                        }
                                    },
                                    color = ContestColors.AccentGreen,
                                    fontSize = 13.sp,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(q.question.explanationFr, color = ContestColors.OnSurface.copy(alpha = 0.88f), fontSize = 13.sp, lineHeight = 19.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    val notAnswered = exam.questions.indices.filter { i ->
                        if (exam.questions[i].question.type == QType.TEXT) (ctext[i] ?: "").isBlank() else (csingle[i] ?: emptyList()).isEmpty()
                    }
                    if (notAnswered.isNotEmpty()) {
                        Text(
                            "Sans réponse : " + notAnswered.map { it + 1 }.joinToString(", "),
                            color = ContestColors.AccentAmber,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteStrip(
    totalCount: Int,
    answered: (Int) -> Boolean,
    currentIndex: Int,
    accent: Color,
    onNavigate: (Int) -> Unit,
) {
    val rows = (totalCount + 9) / 10
    Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
        repeat(rows) { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (c in 0 until 10) {
                    val i = r * 10 + c
                    if (i >= totalCount) break
                    val done = answered(i)
                    val isCurrent = i == currentIndex
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                when {
                                    isCurrent -> accent
                                    done -> accent.copy(alpha = 0.22f)
                                    else -> ContestColors.SurfaceHigh
                                },
                            )
                            .clickable { onNavigate(i) }
                            .padding(vertical = 7.dp)
                            .wrapContentWidth(),
                    ) {
                        Text(
                            "${i + 1}",
                            color = ContestColors.OnSurface,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
