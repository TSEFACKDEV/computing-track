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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klein.ictcomputing.data.Chapter
import com.klein.ictcomputing.data.QType
import com.klein.ictcomputing.data.Question
import com.klein.ictcomputing.data.TaskKind
import com.klein.ictcomputing.ui.Route
import com.klein.ictcomputing.data.textMatches
import com.klein.ictcomputing.focus.FocusMode

/** Une réponse d'utilisateur enregistrée pour une question. */
sealed interface UserAnswer {
    data class Choice(val selected: List<Int>) : UserAnswer
    data class FreeText(val text: String) : UserAnswer
}

fun grade(question: Question, answer: UserAnswer?): Boolean = when (answer) {
    is UserAnswer.Choice -> {
        val normalized = answer.selected.toSet().sorted()
        val target = question.answer.toSet().sorted()
        when (question.type) {
            QType.MULTI -> normalized == target
            else -> normalized == target
        }
    }
    is UserAnswer.FreeText -> textMatches(answer.text, question.accept)
    null -> false
}

@Composable
private fun OptionLabel(q: Question): String = when (q.type) {
    QType.SINGLE -> "Choix unique"
    QType.MULTI -> "Choix multiple"
    QType.TF -> "Vrai / Faux"
    QType.TEXT -> "Saisie libre"
}

@Composable
fun QuizScreen(
    chapterId: String,
    onBack: () -> Unit,
    onNextAfterPass: (com.klein.ictcomputing.ui.Route?) -> Unit,
) {
    val repo = rememberRepo()
    val context = LocalContext.current
    val chapter = repo.chapterById(chapterId)
    if (chapter == null) {
        TopBar("Chapitre indisponible", null, onBack)
        return
    }
    val color = subjectColor(chapter.subject)
    val quiz = chapter.quiz

    var index by remember { mutableStateOf(0) }
    var checked by remember { mutableStateOf(false) }
    var selected by remember(chapterId, index) { mutableStateOf(listOf<Int>()) }
    var textAnswer by remember(chapterId, index) { mutableStateOf("") }
    /** Résultats de chaque question déjà traitée. */
    var results by remember(chapterId) { mutableStateOf(quiz.map { false to listOf<Int>(0) }) }
    var done by remember(chapterId) { mutableStateOf(false) }

    val view = LocalView.current
    var popSeed by remember(chapterId) { mutableIntStateOf(0) }
    var lastOkNow by remember(chapterId) { mutableStateOf(false) }
    var confettiSeed by remember(chapterId) { mutableIntStateOf(0) }

    val q = quiz[index]
    val canValidate = when (q.type) {
        QType.TEXT -> textAnswer.isNotBlank()
        else -> selected.isNotEmpty()
    }

    fun submit() {
        if (checked || !canValidate) return
        val userAnswer: UserAnswer? = when (q.type) {
            QType.TEXT -> UserAnswer.FreeText(textAnswer)
            else -> UserAnswer.Choice(selected)
        }
        val ok = grade(q, userAnswer)
        val newList = results.toMutableList()
        if (index < newList.size) newList[index] = ok to q.answer
        results = newList
        // Coach mémoire : on enregistre chaque résultat ; les ratés iront au rattrapage,
        // les réussis maintiennent la série des notions déjà en carte.
        val inSrs = repo.progress.srs.containsKey(q.id)
        if (!ok || inSrs) repo.recordQuestionResult(q.id, ok)
        Haptics.fire(view, if (ok) Haptics.Kind.GOOD else Haptics.Kind.BAD)
        if (ok) popSeed++
        lastOkNow = ok
        checked = true
    }

    fun next() {
        if (index < quiz.lastIndex) {
            index++
            checked = false
            lastOkNow = false
        } else {
            done = true
            val pctNow = (results.count { it.first } * 100) / quiz.size.coerceAtLeast(1)
            if (pctNow >= 60) { confettiSeed++; Haptics.fire(view, Haptics.Kind.LEVEL_UP) }
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(ContestColors.Background)) {
        TopBar(title = "Test · ${chapter.titleFr}", null, onBack)

        if (!done) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 150.dp),
            ) {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Question ${index + 1}/${quiz.size}", color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        ChipPill(q.topic.ifBlank { "Notion" }, color)
                    }
                    Spacer(Modifier.height(10.dp))
                    Meter((index + if (checked) 1 else 0).toFloat() / quiz.size, color = color, height = 4.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(q.prompt, color = ContestColors.OnSurface, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("—" + OptionLabel(q), color = color, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))

                    if (q.type == QType.TEXT) {
                        OutlinedTextField(
                            value = textAnswer,
                            onValueChange = { textAnswer = it },
                            label = { Text("Ta réponse (exacte, sans bluff)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            keyboardActions = KeyboardActions(onDone = { }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        q.options.forEachIndexed { optionIndex, option ->
                            val isPicked = selected.contains(optionIndex)
                            val showState = checked
                            val correctOption = checked && q.answer.contains(optionIndex)
                            val wrongPick = checked && isPicked && !q.answer.contains(optionIndex)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        when {
                                            showState && correctOption -> ContestColors.AccentGreen.copy(alpha = 0.14f)
                                            showState && wrongPick -> ContestColors.AccentRed.copy(alpha = 0.14f)
                                            isPicked -> color.copy(alpha = 0.16f)
                                            else -> ContestColors.Surface
                                        },
                                    )
                                    .clickable {
                                        if (!checked) {
                                            when (q.type) {
                                                QType.MULTI -> selected = if (isPicked) selected - optionIndex else selected + optionIndex
                                                else -> selected = listOf(optionIndex)
                                            }
                                        }
                                    }
                                    .padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    (if (q.type == QType.TF) "AB" else "ABCD").takeIf { optionIndex < 4 }?.get(optionIndex)?.toString() ?: (optionIndex + 1).toString(),
                                    color = ContestColors.OnSurfaceMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(22.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(option, color = ContestColors.OnSurface, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                                if (correctOption) Text("✓", color = ContestColors.AccentGreen, fontSize = 15.sp)
                                if (wrongPick) Text("✗", color = ContestColors.AccentRed, fontSize = 15.sp)
                            }
                        }
                    }

                    if (checked) {
                        val ok = results[index].first
                        Spacer(Modifier.height(14.dp))
                        CardBox(
                            background = if (ok) ContestColors.AccentGreen.copy(alpha = 0.10f) else ContestColors.AccentRed.copy(alpha = 0.10f),
                            corner = 18.dp,
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    if (ok) "Bonne réponse !" else "Raté — la réponse attendue est : " + correctLabel(q),
                                    color = if (ok) ContestColors.AccentGreen else ContestColors.AccentRed,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Pourquoi :",
                                    color = ContestColors.OnSurfaceMuted,
                                    fontSize = 12.sp,
                                )
                                Text(
                                    q.explanationFr,
                                    color = ContestColors.OnSurface,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            Box(Modifier.align(Alignment.CenterHorizontally)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(ContestColors.SurfaceLow.copy(alpha = 0.98f))
                        .padding(14.dp),
                ) {
                    if (checked && lastOkNow) XpPop(8, seed = popSeed)
                    if (!checked) {
                        Text(
                            "Valider",
                            color = if (canValidate) Color.White else ContestColors.OnSurfaceMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(if (canValidate) ContestColors.AccentRed else ContestColors.SurfaceHigh)
                                .clickable { submit() }
                                .padding(vertical = 12.dp),
                        )
                    } else {
                        Text(
                            "Question suivante",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(ContestColors.AccentRed)
                                .clickable { next() }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        } else {
            val correctCount = results.count { it.first }
            val pct = (correctCount * 100) / quiz.size
            val passed = pct >= 60

            LaunchedEffect(chapterId, correctCount) {
                repo.recordQuiz(
                    chapterId = chapterId,
                    subject = chapter.subject,
                    questionCount = quiz.size,
                    correct = correctCount,
                )
                if (passed) {
                    (context as? android.app.Activity)?.let { FocusMode.finish(it) }
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 140.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (passed) "VALIDÉ" else "Insuffisant",
                            color = if (passed) ContestColors.AccentGreen else ContestColors.AccentRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text("$pct %", color = if (passed) ContestColors.AccentGreen else ContestColors.AccentRed, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Meter(pct / 100f, color = if (passed) ContestColors.AccentGreen else ContestColors.AccentRed)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Seuil Huawei : 60 %",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    if (passed) {
                        val nextChapter = repo.nextChapter(repo.progress.selectedPhase, chapter.subject)
                        val tasks = repo.dailyTasks()
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Chapitre suivant",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.large)
                                    .background(ContestColors.AccentRed)
                                    .clickable {
                                        val route = when {
                                            tasks.firstOrNull()?.done != true && tasks.firstOrNull()?.kind == TaskKind.CHAPTER ->
                                                tasks.firstOrNull()?.chapter?.let { Route.Course(it.id) }
                                            else -> null
                                        } ?: nextChapter?.let { Route.Course(it.id) } ?: Route.Revise
                                        onNextAfterPass(route)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                            Text(
                                "Retour",
                                color = ContestColors.OnSurfaceMuted,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.large)
                                    .background(ContestColors.SurfaceHigh)
                                    .clickable { onNextAfterPass(null) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    } else {
                        Text(
                            "Relis les points posés ci-dessous, puis retente le chapitre : la répétition crée la mémoire.",
                            color = ContestColors.OnSurfaceMuted,
                            fontSize = 13.sp,
                        )
                        val wrongCount = results.count { !it.first }
                        if (wrongCount > 0) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "🔥 Coach · $wrongCount notion(s) ratée(s)",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.large)
                                    .background(ContestColors.AccentAmber)
                                    .clickable { onNextAfterPass(Route.Weak) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Correction détaillée", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    quiz.forEachIndexed { i, question ->
                        val ok = results.getOrNull(i)?.first ?: false
                        ReviewCard(question, ok)
                        Spacer(Modifier.height(10.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Rejouer le test",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(ContestColors.SurfaceHigh)
                            .clickable {
                                done = false
                                checked = false
                                index = 0
                                selected = emptyList()
                                textAnswer = ""
                            }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
    ConfettiOverlay(Modifier.fillMaxSize(), trigger = confettiSeed)
    }
}

internal fun letterLabel(index: Int): String = if (index == 0) "VRAI" else "FAUX"

internal fun correctLabel(q: Question): String = when (q.type) {
    QType.TEXT -> q.sampleAnswer ?: q.accept.firstOrNull() ?: ""
    QType.TF -> q.answer.joinToString(" • ") { i -> q.options.getOrNull(i)?.let { if (it.equals("True", true)) "VRAI (True)" else "FAUX (False)" } ?: "" }
    else -> q.answer.joinToString(" • ") { i -> (if (i < q.options.size) "${'A' + i}" else "") + " " + (q.options.getOrNull(i) ?: "") }
}

@Composable
private fun ReviewCard(question: Question, ok: Boolean) {
    CardBox(background = if (ok) ContestColors.AccentGreen.copy(alpha = 0.06f) else ContestColors.AccentRed.copy(alpha = 0.08f)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (ok) "✓" else "✗", color = if (ok) ContestColors.AccentGreen else ContestColors.AccentRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(question.topic, color = ContestColors.OnSurfaceMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(question.prompt, color = ContestColors.OnSurface, fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("Réponse : " + question.answer.joinToString(" • ") { index -> (question.options.getOrNull(index) ?: (if (question.type == QType.TF) letterLabel(index) else "")) }, color = ContestColors.AccentGreen, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(question.explanationFr, color = ContestColors.OnSurface.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}
