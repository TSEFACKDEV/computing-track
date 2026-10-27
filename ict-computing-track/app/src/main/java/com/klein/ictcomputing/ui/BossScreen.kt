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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Arène Boss — 12 questions, 3 cœurs, chrono 4 min. Chaque bonne réponse enlève 1 HP au boss. */
@Composable
fun BossScreen(phaseId: String, subjectId: String, onBack: () -> Unit) {
    val repo = rememberRepo()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val accent = subjectColor(subjectId)

    // Le lot de questions est figé au montage ; « Relancer » en recharge un nouveau.
    var questions by remember(phaseId, subjectId) { mutableStateOf(repo.bossRounds(phaseId, subjectId, count = 12)) }
    val bossName = remember(subjectId) { bossTitle(subjectId) }
    val bossEmoji = remember(subjectId) { bossEmoji(subjectId) }

    var stage by remember(phaseId, subjectId) { mutableStateOf(BossStage.INTRO) }
    var idx by remember { mutableIntStateOf(0) }
    var picks by remember { mutableStateOf(listOf<Int>()) }
    var textAns by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(false) }
    var lastOk by remember { mutableStateOf(false) }
    var hp by remember { mutableIntStateOf(8) }
    var lives by remember { mutableIntStateOf(3) }
    var correct by remember { mutableIntStateOf(0) }
    var bossFlash by remember { mutableStateOf(false) }
    var popSeed by remember { mutableIntStateOf(0) }
    var confettiSeed by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableLongStateOf(240L) }

    fun maxHp(total: Int): Int = (total * 2) / 3 + 1
    fun resetStep() { picks = emptyList(); textAns = ""; checked = false; lastOk = false }

    fun startFight(newLot: List<com.klein.ictcomputing.data.Question>?) {
        newLot?.let { questions = it }
        hp = maxHp(questions.size)
        idx = 0
        correct = 0
        lives = 3
        secondsLeft = 240
        confettiSeed = 0
        bossFlash = false
        resetStep()
        stage = BossStage.FIGHT
    }

    fun finishFight(defeated: Boolean) {
        repo.recordBoss(phaseId, subjectId, correct, questions.size, defeated)
        stage = if (defeated) BossStage.END else BossStage.FAIL
        if (defeated) { confettiSeed++; Haptics.fire(view, Haptics.Kind.BOSS_DOWN) }
    }

    // chrono global
    LaunchedEffect(stage) {
        if (stage == BossStage.FIGHT) {
            while (secondsLeft > 0) { delay(1000); secondsLeft-- }
            if (secondsLeft <= 0 && stage == BossStage.FIGHT) finishFight(false)
        }
    }

    val q = questions.getOrNull(idx)
    val mm = (secondsLeft / 60).toString().padStart(2, '0')
    val ss = (secondsLeft % 60).toString().padStart(2, '0')
    val bossCanValidate = q != null && when (q.type) {
        QType.TEXT -> textAns.isNotBlank()
        else -> picks.isNotEmpty()
    }

    Column(Modifier.fillMaxSize().background(ContestColors.Background)) {
        TopBar("Arène", bossName, onBack)

        when (stage) {
            BossStage.INTRO -> {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.padding(28.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(bossEmoji, fontSize = 58.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(bossName, color = ContestColors.OnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Phase $phaseId · $subjectId", color = ContestColors.OnSurfaceMuted, fontSize = 13.sp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "${questions.size} questions · 3 ❤ · 4:00 chrono\nVise ${maxHp(questions.size)} bonnes réponses pour le faire tomber.",
                            color = ContestColors.OnSurfaceMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                        val best = repo.progress.bosses["$phaseId|$subjectId"]
                        if (best != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (best.defeated) "Déjà vaincu ${best.wins}× — dernier score ${best.lastCorrect}/${questions.size}"
                                else "Meilleur essai : ${best.lastCorrect}/${questions.size}",
                                color = ContestColors.AccentAmber,
                                fontSize = 12.sp,
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "⚔️ Commencer le combat",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(ContestColors.AccentRed)
                                .clickable { startFight(null) }
                                .padding(vertical = 12.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Retour",
                            color = ContestColors.OnSurfaceMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onBack() }.padding(vertical = 8.dp),
                        )
                    }
                }
            }
            BossStage.END, BossStage.FAIL -> {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.padding(28.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val won = stage == BossStage.END
                        Text(if (won) bossEmoji else "💀", fontSize = 52.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (won) "$bossName est à terre !" else "Le boss a résisté…",
                            color = if (won) ContestColors.AccentGreen else ContestColors.AccentRed,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Score : $correct/${questions.size}", color = ContestColors.OnSurfaceMuted, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "+${correct * 10 + if (won) 200 else 40} XP",
                            color = ContestColors.AccentAmber,
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            if (won) "Recombattre" else "Retenter",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(ContestColors.AccentRed)
                                .clickable { startFight(repo.bossRounds(phaseId, subjectId, count = 12)) }
                                .padding(vertical = 12.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Retour à la stats",
                            color = ContestColors.OnSurfaceMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { onBack() }.padding(vertical = 8.dp),
                        )
                    }
                    if (stage == BossStage.END) ConfettiOverlay(Modifier.fillMaxSize(), trigger = confettiSeed)
                }
            }
            BossStage.FIGHT -> {
                if (q == null) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("Lot de combat vide — relance l'arène.", color = ContestColors.OnSurfaceMuted, fontSize = 14.sp)
                    }
                    Box(Modifier.fillMaxWidth().navigationBarsPadding().background(ContestColors.SurfaceLow).padding(14.dp)) {
                        Text(
                            "Retour",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.large)
                                .background(ContestColors.SurfaceHigh)
                                .clickable { onBack() }
                                .padding(vertical = 12.dp),
                        )
                    }
                } else {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Column(Modifier.padding(horizontal = 18.dp)) {
                            Spacer(Modifier.height(10.dp))
                            // ---------- HUD boss ----------
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$mm:$ss", color = if (secondsLeft <= 45) ContestColors.AccentRed else ContestColors.OnSurfaceMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text("❤".repeat(lives.coerceAtLeast(1)), color = ContestColors.AccentRed, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(bossEmoji, fontSize = 26.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("$bossName  ·  HP ${hp.coerceAtLeast(0)}/${questions.size}", color = ContestColors.OnSurfaceMuted, fontSize = 11.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Meter(hp.toFloat() / questions.size.coerceAtLeast(1), color = if (bossFlash) ContestColors.AccentRed else ContestColors.AccentAmber, height = 9.dp)
                                }
                            }
                            Spacer(Modifier.height(14.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ChipPill(q.topic.ifBlank { "Attaque" }, accent)
                                Spacer(Modifier.weight(1f))
                                Text("Question ${idx + 1}/${questions.size}", color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(10.dp))
                            Meter(idx.toFloat() / questions.size.coerceAtLeast(1), color = accent, height = 3.dp)
                            Spacer(Modifier.height(14.dp))
                            Text(q.prompt, color = ContestColors.OnSurface, fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(12.dp))

                            if (q.type == QType.TEXT) {
                                OutlinedTextField(
                                    value = textAns,
                                    onValueChange = { textAns = it },
                                    label = { Text("Ta réponse") },
                                    enabled = !checked,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                    keyboardActions = KeyboardActions(onDone = { }),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                q.options.forEachIndexed { oi, option ->
                                    val isPicked = picks.contains(oi)
                                    val goodOpt = checked && q.answer.contains(oi)
                                    val badPick = checked && isPicked && !q.answer.contains(oi)
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(
                                                when {
                                                    goodOpt -> ContestColors.AccentGreen.copy(alpha = 0.14f)
                                                    badPick -> ContestColors.AccentRed.copy(alpha = 0.14f)
                                                    isPicked -> accent.copy(alpha = 0.16f)
                                                    else -> ContestColors.Surface
                                                },
                                            )
                                            .clickable {
                                                if (!checked) {
                                                    when (q.type) {
                                                        QType.MULTI -> picks = if (isPicked) picks - oi else picks + oi
                                                        else -> picks = listOf(oi)
                                                    }
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            ("ABCD").takeIf { oi < 4 }?.get(oi)?.toString() ?: (oi + 1).toString(),
                                            color = ContestColors.OnSurfaceMuted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.width(20.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(option, color = ContestColors.OnSurface, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                                        if (goodOpt) Text("✓", color = ContestColors.AccentGreen, fontSize = 15.sp)
                                        if (badPick) Text("✗", color = ContestColors.AccentRed, fontSize = 15.sp)
                                    }
                                }
                            }

                            if (checked) {
                                Spacer(Modifier.height(12.dp))
                                val okColor = if (lastOk) ContestColors.AccentGreen else ContestColors.AccentRed
                                CardBox(background = okColor.copy(alpha = 0.10f), corner = 18.dp) {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(
                                            if (lastOk) "💥 -1 HP pour le boss !" else "💥 L'attaque du boss te coûte 1 ❤",
                                            color = okColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                        )
                                        if (!lastOk) {
                                            Spacer(Modifier.height(4.dp))
                                            Text("Attendu : " + correctLabel(q), color = ContestColors.OnSurfaceMuted, fontSize = 12.sp)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(q.explanationFr, color = ContestColors.OnSurface, fontSize = 14.sp, lineHeight = 21.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    // ---------- Barre basse ----------
                    Box(Modifier.fillMaxWidth().navigationBarsPadding().background(ContestColors.SurfaceLow.copy(alpha = 0.98f)).padding(14.dp)) {
                        Column {
                            if (checked && lastOk) XpPop(10, seed = popSeed)
                            val label = when {
                                !checked -> "Frapper 💥"
                                lives <= 0 -> "Voir le verdict"
                                hp <= 0 -> "Voir le verdict"
                                idx + 1 >= questions.size -> "Voir le verdict"
                                else -> "Attaque suivante ⚔️"
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
                                    .background(if (bossCanValidate || checked) ContestColors.AccentRed else ContestColors.SurfaceHigh)
                                    .clickable {
                                        if (!checked) {
                                            if (!bossCanValidate) return@clickable
                                            val user = if (q.type == QType.TEXT) UserAnswer.FreeText(textAns) else UserAnswer.Choice(picks)
                                            val ok = grade(q, user)
                                            correct++
                                            if (ok) {
                                                popSeed++
                                                hp--
                                                Haptics.fire(view, Haptics.Kind.GOOD)
                                            } else Haptics.fire(view, Haptics.Kind.BAD)
                                            checked = true
                                            lastOk = ok
                                            if (ok && hp <= 0) finishFight(true)
                                        } else if (lives <= 0) {
                                            finishFight(false)
                                        } else if (idx + 1 >= questions.size) {
                                            finishFight(hp <= 0)
                                        } else {
                                            idx++
                                            resetStep()
                                            if (!lastOk) {
                                                lives--
                                                bossFlash = true
                                                scope.launch { delay(300); bossFlash = false }
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
        ConfettiOverlay(Modifier.fillMaxSize(), trigger = confettiSeed)
    }
}

private enum class BossStage { INTRO, FIGHT, END, FAIL }

private fun bossTitle(subject: String): String = when (subject.lowercase()) {
    "openeuler" -> "Célestin le Dragon d'openEuler"
    "opengauss" -> "Tartare le Gardien d'openGauss"
    else -> "Granit le Colosse ARM"
}

private fun bossEmoji(subject: String): String = when (subject.lowercase()) {
    "openeuler" -> "🐉"
    "opengauss" -> "🗄️"
    else -> "🦾"
}
