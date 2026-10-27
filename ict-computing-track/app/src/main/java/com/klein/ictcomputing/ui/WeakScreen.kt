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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klein.ictcomputing.data.QType
import com.klein.ictcomputing.data.Question

/**
 * Coach Mémoire — la salle de rattrapage : toute notion ratée revient ici
 * jusqu'à 5 succès consécutifs (statut « maîtrisée »).
 */
@Composable
fun WeakScreen(onBack: () -> Unit) {
    val repo = rememberRepo()
    val view = LocalView.current
    var queue by remember { mutableStateOf(repo.coachDueIds()) }
    var idx by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf(listOf<Int>()) }
    var textAns by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var lastOk by remember { mutableStateOf(false) }
    var attempts by remember { mutableIntStateOf(0) }
    var good by remember { mutableIntStateOf(0) }
    var mastered by remember { mutableIntStateOf(0) }
    var popSeed by remember { mutableIntStateOf(0) }
    var confettiSeed by remember { mutableIntStateOf(0) }

    fun resetStep() {
        selected = emptyList()
        textAns = ""
        checked = false
        lastOk = false
    }

    fun reload() {
        queue = repo.coachDueIds()
        idx = 0
        resetStep()
    }

    val q = queue.getOrNull(idx)?.let { repo.coachQuestion(it) }
    val color = questionAccent(repo, q)
    val coachCanValidate = q != null && when (q.type) {
        QType.TEXT -> textAns.isNotBlank()
        else -> selected.isNotEmpty()
    }

    Column(Modifier.fillMaxSize().background(ContestColors.Background)) {
        TopBar("Coach · Mémoire", "Notions ratées → maîtrise", onBack)

        if (queue.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.padding(26.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🔥", fontSize = 52.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (attempts > 0) "Terminé : $mastered maîtrisée(s) · $good réussie(s) 👏"
                        else "Aucune notion à rattraper. 💪",
                        color = ContestColors.OnSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Le Coach réveille les questions ratées, une par une, jusqu'à ce que tu les enchaînes 5 fois sans faute.",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                    if (attempts > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "+$good réponses gagnantes · XP cumulée",
                            color = ContestColors.AccentGreen,
                            fontSize = 13.sp,
                        )
                    }
                }
                ConfettiOverlay(Modifier.fillMaxSize(), trigger = confettiSeed)
            }
            Box(Modifier.fillMaxWidth().navigationBarsPadding().background(ContestColors.SurfaceLow).padding(18.dp)) {
                Text(
                    "Retour",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(ContestColors.SurfaceHigh)
                        .clickable { onBack() }
                        .padding(vertical = 12.dp),
                )
            }
            return@Column
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 110.dp)) {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Notion ${idx + 1}/${queue.size}", color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    ChipPill(q?.topic?.ifBlank { "Notion" } ?: "Notion", color)
                }
                Spacer(Modifier.height(10.dp))
                Meter(idx.toFloat() / queue.size.coerceAtLeast(1), color = color, height = 4.dp)
                Spacer(Modifier.height(16.dp))
            }

            if (q == null) {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Text(
                        "Question introuvable (contenu mis à jour) — passe à la suivante.",
                        color = ContestColors.OnSurfaceMuted,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                }
            } else {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    Text(q.prompt, color = ContestColors.OnSurface, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("—" + coachTypeLabel(q), color = color, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))

                    if (q.type == QType.TEXT) {
                        OutlinedTextField(
                            value = textAns,
                            onValueChange = { textAns = it },
                            label = { Text("Ta réponse (exacte, sans bluff)") },
                            enabled = !checked,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            keyboardActions = KeyboardActions(onDone = { }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        q.options.forEachIndexed { optionIndex, option ->
                            val isPicked = selected.contains(optionIndex)
                            val correctOption = checked && q.answer.contains(optionIndex)
                            val wrongPick = checked && isPicked && !q.answer.contains(optionIndex)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        when {
                                            correctOption -> ContestColors.AccentGreen.copy(alpha = 0.14f)
                                            wrongPick -> ContestColors.AccentRed.copy(alpha = 0.14f)
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
                                    ("ABCD").takeIf { optionIndex < 4 }?.get(optionIndex)?.toString() ?: (optionIndex + 1).toString(),
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
                        Spacer(Modifier.height(14.dp))
                        val okColor = if (lastOk) ContestColors.AccentGreen else ContestColors.AccentRed
                        CardBox(background = okColor.copy(alpha = 0.10f), corner = 18.dp) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    if (lastOk) "Bonne réponse !" else "Raté — la réponse attendue : " + correctLabel(q),
                                    color = okColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    q.explanationFr,
                                    color = ContestColors.OnSurface,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                )
                                Spacer(Modifier.height(6.dp))
                                val streakNow = repo.progress.srs[q.id]?.streak
                                Text(
                                    when {
                                        repo.progress.clears.contains(q.id) -> "🏅 Notion maîtrisée !"
                                        streakNow != null && streakNow > 0 -> "Série : $streakNow/5 — on la garde de côté"
                                        else -> "Reviendra dès le prochain passage"
                                    },
                                    color = ContestColors.OnSurfaceMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---------- Barre basse ----------
        Box(Modifier.fillMaxWidth().navigationBarsPadding().background(ContestColors.SurfaceLow.copy(alpha = 0.98f)).padding(14.dp)) {
            Column {
                if (checked && lastOk) {
                    XpPop(
                        if (repo.progress.clears.contains(q?.id ?: "")) 40 else 12,
                        seed = popSeed,
                        big = repo.progress.clears.contains(q?.id ?: ""),
                    )
                }
                val label = when {
                    q == null -> if (idx + 1 < queue.size) "Suivant" else "Terminé"
                    !checked -> "Valider"
                    repo.progress.clears.contains(q.id) -> if (idx + 1 < queue.size) "Notion suivante ✅" else "Session terminée 🎉"
                    q.id in repo.progress.srs -> "Rejouer cette notion (${(repo.progress.srs[q.id]?.streak ?: 0) + 1}/5)"
                    idx + 1 < queue.size -> "Question suivante"
                    repo.coachCount() > 0 -> "Recommencer (${repo.coachCount()})"
                    else -> "Terminé"
                }
                Text(
                    label,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(if (coachCanValidate || checked) ContestColors.AccentRed else ContestColors.SurfaceHigh)
                        .clickable {
                            if (q == null) {
                                // sauter la question introuvable
                                if (idx + 1 < queue.size) {
                                    idx++; resetStep()
                                } else reload()
                            } else if (!checked) {
                                if (!coachCanValidate) return@clickable
                                val ok = when (q.type) {
                                    QType.TEXT -> grade(q, UserAnswer.FreeText(textAns))
                                    else -> grade(q, UserAnswer.Choice(selected))
                                }
                                Haptics.fire(view, if (ok) Haptics.Kind.GOOD else Haptics.Kind.BAD)
                                attempts++
                                if (ok) {
                                    good++
                                    popSeed++
                                    if (!repo.progress.srs.containsKey(q.id)) mastered++
                                }
                                repo.recordQuestionResult(q.id, ok)
                                checked = true
                                lastOk = ok
                            } else {
                                // une notion toujours en carte (streak < 5) se rejoue immédiatement ;
                                // une notion maîtrisée (carte retirée par recordQuestionResult) libère l'index
                                val stillInSrs = repo.progress.srs.containsKey(q.id)
                                if (stillInSrs) {
                                    resetStep()
                                } else {
                                    mastered = (mastered + 1).coerceAtMost(queue.size)
                                    if (idx + 1 < queue.size) {
                                        idx++; resetStep()
                                    } else if (repo.coachCount() > 0) {
                                        reload()
                                    } else {
                                        confettiSeed++
                                        reload()
                                    }
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                )
            }
        }
        ConfettiOverlay(Modifier.fillMaxSize(), trigger = confettiSeed)
    }
}

private fun coachTypeLabel(q: Question): String = when (q.type) {
    QType.SINGLE -> "choisir une option"
    QType.MULTI -> "plusieurs réponses possibles"
    QType.TF -> "vrai ou faux"
    QType.TEXT -> "réponse courte"
}

/** couleur du sujet d'une question (via l'index des pools) */
private fun questionAccent(
    repo: com.klein.ictcomputing.data.AppRepository,
    q: Question?,
): Color {
    if (q == null) return ContestColors.AccentCyan
    val subject = repo.questionPool.entries.firstOrNull { entry -> entry.value.any { it.id == q.id } }?.key
        ?.substringAfter('|') ?: "openEuler"
    return subjectColor(subject)
}
