package com.klein.ictcomputing.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.klein.ictcomputing.alarm.AlarmSystem
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.time.LocalDate
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

val SUBJECTS = listOf("openEuler", "openGauss", "Kunpeng")
val SUBJECT_WEIGHTS = mapOf("openEuler" to 50, "openGauss" to 30, "Kunpeng" to 20)

/** Sorting key used by the course/quiz flow: openEuler first, then openGauss, then Kunpeng. */
fun subjectOrder(subject: String): Int = SUBJECTS.indexOf(subject)

/* ------------------------------------------------------------------ */
/* Content loading from assets/content (JSON files authored per phase) */
/* ------------------------------------------------------------------ */

class ContentLoader(private val context: Context) {

    private val chapterCache = HashMap<String, Chapter>()

    fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    fun loadPhases(): List<Phase> = try {
        val root = JSONObject(readAsset("content/index.json"))
        root.getJSONArray("phases").let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Phase(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    subtitle = o.optString("subtitle"),
                    description = o.optString("description"),
                    durationMin = o.optInt("duration_min", 60),
                    questionCount = o.optInt("question_count", 60),
                )
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun subjectExists(phase: String, subject: String): Boolean =
        try {
            readAsset("content/$phase/$subject/index.json")
            true
        } catch (e: Exception) {
            false
        }

    fun loadSubjectIndex(phase: String, subject: String): SubjectIndex? = try {
        val o = JSONObject(readAsset("content/$phase/$subject/index.json"))
        val chapters = o.optJSONArray("chapters") ?: throw IllegalArgumentException("no chapters")
        val metas = (0 until chapters.length()).map { i ->
            val c = chapters.getJSONObject(i)
            ChapterMeta(
                id = c.optString("id"),
                file = c.optString("file"),
                title = c.optString("title"),
                minutes = c.optInt("minutes", 30),
            )
        }
        SubjectIndex(
            phase = o.optString("phase"),
            subject = o.optString("subject"),
            name = o.optString("name"),
            tagline = o.optString("tagline"),
            weight = o.optInt("weight", SUBJECT_WEIGHTS[subject] ?: 0),
            chapters = metas,
        )
    } catch (e: Exception) {
        null
    }

    fun allSubjects(phase: String): List<SubjectIndex> = SUBJECTS.mapNotNull { s ->
        if (subjectExists(phase, s)) loadSubjectIndex(phase, s) else null
    }

    private fun parseTable(t: JSONObject?): CourseTable? {
        t ?: return null
        val h = t.optJSONArray("headers")
        val headers = if (h != null) (0 until h.length()).map { i -> h.optString(i) } else emptyList()
        val rows = t.optJSONArray("rows")?.let { rowsArr ->
            (0 until rowsArr.length()).map { r ->
                val row = rowsArr.optJSONArray(r)
                if (row != null) (0 until row.length()).map { c -> row.optString(c) } else emptyList()
            }
        } ?: emptyList()
        return CourseTable(headers = headers, rows = rows)
    }

    fun parseChapter(json: String): Chapter {
        val o = JSONObject(json)
        val quiz = o.optJSONArray("quiz")?.let { arr ->
            (0 until arr.length()).map { i ->
                val q = arr.getJSONObject(i)
                Question(
                    id = q.optString("id"),
                    type = when (q.optString("type")) {
                        "multi" -> QType.MULTI
                        "tf" -> QType.TF
                        "text" -> QType.TEXT
                        else -> QType.SINGLE
                    },
                    prompt = q.optString("prompt"),
                    options = q.optJSONArray("options")?.let { a ->
                        (0 until a.length()).map { j -> a.optString(j) }
                    } ?: emptyList(),
                    answer = q.optJSONArray("answer")?.let { a ->
                        (0 until a.length()).map { j -> a.optInt(j) }
                    } ?: emptyList(),
                    accept = q.optJSONArray("accept")?.let { a ->
                        (0 until a.length()).map { j -> a.optString(j) }.filter { it.isNotBlank() }
                    } ?: emptyList(),
                    sampleAnswer = q.optString("sample_answer", "").takeIf { it.isNotEmpty() },
                    explanationFr = q.optString("explanation_fr"),
                    topic = q.optString("topic"),
                )
            }
        } ?: emptyList()
        val sections = (o.optJSONArray("sections") ?: JSONArray()).let { arr ->
            (0 until arr.length()).map { i ->
                val s = arr.getJSONObject(i)
                CourseSection(
                    heading = s.optString("heading"),
                    body = s.optString("body"),
                    bullets = s.optJSONArray("bullets")?.let { a ->
                        (0 until a.length()).map { j -> a.optString(j) }
                    } ?: emptyList(),
                    code = s.optJSONArray("code")?.let { a ->
                        (0 until a.length()).map { j -> a.optString(j) }
                    } ?: emptyList(),
                    table = parseTable(s.optJSONObject("table")),
                    tip = s.optString("tip", "").takeIf { it.isNotBlank() },
                    diagram = s.optString("diagram", "").takeIf { it.isNotBlank() },
                    exercises = s.optJSONArray("exercises")?.let { a ->
                        (0 until a.length()).mapNotNull { j ->
                            val e = a.optJSONObject(j) ?: return@mapNotNull null
                            val prompt = e.optString("prompt", "")
                            val correction = e.optString("correction", "")
                            if (prompt.isBlank() || correction.isBlank()) null
                            else CourseExercise(prompt = prompt, correction = correction)
                        }
                    } ?: emptyList(),
                )
            }
        }
        return Chapter(
            id = o.optString("id"),
            phase = o.optString("phase"),
            subject = o.optString("subject"),
            number = o.optInt("number", 1),
            titleFr = o.optString("title_fr"),
            titleEn = o.optString("title_en"),
            minutes = o.optInt("minutes", 30),
            source = o.optString("source"),
            objectives = o.optJSONArray("objectives")?.let { a ->
                (0 until a.length()).map { j -> a.optString(j) }
            } ?: emptyList(),
            sections = sections,
            keyPoints = o.optJSONArray("key_points")?.let { a ->
                (0 until a.length()).map { j -> a.optString(j) }
            } ?: emptyList(),
            traps = o.optJSONArray("traps")?.let { a ->
                (0 until a.length()).map { j -> a.optString(j) }
            } ?: emptyList(),
            quiz = quiz,
        )
    }

    fun chapter(phase: String, subject: String, file: String): Chapter? {
        val key = "$phase/$subject/$file"
        chapterCache[key]?.let { return it }
        return try {
            val ch = parseChapter(readAsset("content/$phase/$subject/$file"))
            chapterCache[key] = ch
            ch
        } catch (e: Exception) {
            null
        }
    }

    fun allChapters(phase: String, subject: String): List<Chapter> =
        loadSubjectIndex(phase, subject)?.chapters?.mapNotNull { chapter(phase, subject, it.file) } ?: emptyList()

    fun questionBank(phase: String, subject: String): List<ExamQuestion> =
        allChapters(phase, subject).flatMap { ch -> ch.quiz.map { ExamQuestion(it, subject, ch.id) } }
}

/* ------------------------------------------------------------------ */
/* Answer normalization / matching for free-text questions            */
/* ------------------------------------------------------------------ */

fun normalizeAnswer(input: String): String {
    var s = input.trim().lowercase()
    s = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    s = s.replace('œ', 'o').replace('æ', 'a')
    s = s.replace(Regex("[^a-z0-9\\-+*%/\\s]"), " ")
    s = s.replace(Regex("\\s+"), " ").trim()
    return s
}

private fun digitsOf(s: String): String = s
    .replace(Regex("[^0-9\\-+]"), " ").replace(Regex("\\s+"), " ").trim()

private fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    var prev = IntArray(b.length + 1) { it }
    val curr = IntArray(b.length + 1)
    for (i in 1..a.length) {
        curr[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
        }
        val tmp = prev
        prev = curr
        for (k in curr.indices) prev[k] = curr[k]
    }
    return prev[b.length]
}

fun textMatches(input: String, accept: List<String>): Boolean {
    val n = normalizeAnswer(input)
    if (n.isEmpty()) return false
    val nDigits = digitsOf(n)
    for (a in accept) {
        val an = normalizeAnswer(a)
        if (an.isEmpty()) continue
        if (an == n) return true
        val aDigits = digitsOf(an)
        if (aDigits.isNotEmpty() && aDigits == nDigits && aDigits.length >= 1) return true
        val tolerance = if (an.length > 10) 2 else 1
        if (abs(an.length - n.length) <= 2 && levenshtein(an, n) <= tolerance) return true
    }
    return false
}

/* ------------------------------------------------------------------ */
/* Persistent progress stored as JSON in SharedPreferences            */
/* ------------------------------------------------------------------ */

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("ict_progress", Context.MODE_PRIVATE)

    fun load(): Progress {
        val raw = prefs.getString("progress", "") ?: ""
        if (raw.isBlank()) return Progress()
        return try {
            val o = JSONObject(raw)
            val chapters = o.optJSONObject("chapters")?.let { c ->
                val m = mutableMapOf<String, ChapterProgress>()
                c.keys().forEach { id ->
                    val v = c.getJSONObject(id)
                    m[id] = ChapterProgress(
                        read = v.optBoolean("read"),
                        bestScore = v.optInt("best", 0),
                        passed = v.optBoolean("passed"),
                        attempts = v.optInt("attempts"),
                    )
                }
                m
            } ?: emptyMap()
            val daily = o.optJSONObject("daily")?.let { d ->
                val m = mutableMapOf<String, Boolean>()
                d.keys().forEach { k -> m[k] = d.optBoolean(k) }
                m
            } ?: emptyMap()
            val mocks = o.optJSONObject("mocks")?.let { c ->
                val m = mutableMapOf<String, MockResult>()
                c.keys().forEach { id ->
                    val v = c.getJSONObject(id)
                    m[id] = MockResult(
                        score = v.optInt("score"),
                        passed = v.optBoolean("passed"),
                        date = v.optString("date"),
                        correct = v.optInt("correct"),
                        total = v.optInt("total"),
                    )
                }
                m
            } ?: emptyMap()
            val qstats = o.optJSONObject("question_stats")?.let { all ->
                val m = mutableMapOf<String, QStat>()
                all.keys().forEach { k -> m[k] = QStat(correct = all.getJSONObject(k).optInt("correct"), wrong = all.getJSONObject(k).optInt("wrong")) }
                m.toMap()
            } ?: emptyMap()
            val srsMap = o.optJSONObject("srs")?.let { all ->
                val m = mutableMapOf<String, SrsCard>()
                all.keys().forEach { k -> m[k] = SrsCard(due = all.getJSONObject(k).optString("due"), streak = all.getJSONObject(k).optInt("streak")) }
                m.toMap()
            } ?: emptyMap()
            val clears = o.optJSONArray("clears")?.let { arr ->
                (0 until arr.length()).map { i -> arr.optString(i) }
            } ?: emptyList()
            val bossMap = o.optJSONObject("bosses")?.let { all ->
                val m = mutableMapOf<String, BossResult>()
                all.keys().forEach { k ->
                    val v = all.getJSONObject(k)
                    m[k] = BossResult(defeated = v.optBoolean("defeated"), wins = v.optInt("wins"), lastCorrect = v.optInt("last_correct"))
                }
                m.toMap()
            } ?: emptyMap()
            val phaseExams = o.optJSONArray("phase_exams")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val v = arr.getJSONObject(i)
                    PhaseResult(
                        phase = v.optString("phase"),
                        score = v.optInt("score"),
                        passed = v.optBoolean("passed"),
                        date = v.optString("date"),
                        correct = v.optInt("correct"),
                        total = v.optInt("total"),
                    )
                }
            } ?: emptyList()
            Progress(
                selectedPhase = o.optString("selected_phase", "HCIA"),
                onboarded = o.optBoolean("onboarded"),
                chapters = chapters,
                daily = daily,
                streak = o.optInt("streak"),
                lastStudyDay = o.optString("last_study_day").takeIf { it.isNotEmpty() },
                mocks = mocks,
                phaseExams = phaseExams,
                totalQuestions = o.optInt("total_questions"),
                totalCorrect = o.optInt("total_correct"),
                xp = o.optInt("xp"),
                questionStats = qstats,
                srs = srsMap,
                clears = clears,
                bosses = bossMap,
            )
        } catch (e: Exception) {
            Progress()
        }
    }

    fun save(p: Progress) {
        try {
            val o = JSONObject()
            o.put("selected_phase", p.selectedPhase)
            o.put("onboarded", p.onboarded)
            val chapters = JSONObject()
            p.chapters.forEach { (id, cp) ->
                chapters.put(id, JSONObject().put("read", cp.read).put("best", cp.bestScore)
                    .put("passed", cp.passed).put("attempts", cp.attempts))
            }
            o.put("chapters", chapters)
            val daily = JSONObject()
            p.daily.forEach { (k, v) -> daily.put(k, v) }
            o.put("daily", daily)
            o.put("streak", p.streak)
            o.put("last_study_day", p.lastStudyDay ?: "")
            val mocks = JSONObject()
            p.mocks.forEach { (id, mr) ->
                mocks.put(id, JSONObject().put("score", mr.score).put("passed", mr.passed)
                    .put("date", mr.date).put("correct", mr.correct).put("total", mr.total))
            }
            o.put("mocks", mocks)
            val exams = JSONArray()
            p.phaseExams.forEach { pe ->
                exams.put(JSONObject().put("phase", pe.phase).put("score", pe.score)
                    .put("passed", pe.passed).put("date", pe.date)
                    .put("correct", pe.correct).put("total", pe.total))
            }
            o.put("phase_exams", exams)
            o.put("total_questions", p.totalQuestions)
            o.put("total_correct", p.totalCorrect)
            o.put("xp", p.xp)
            val qstats = JSONObject()
            p.questionStats.forEach { (id, s) ->
                qstats.put(id, JSONObject().put("correct", s.correct).put("wrong", s.wrong))
            }
            o.put("question_stats", qstats)
            val srs = JSONObject()
            p.srs.forEach { (id, c) -> srs.put(id, JSONObject().put("due", c.due).put("streak", c.streak)) }
            o.put("srs", srs)
            o.put("clears", JSONArray().also { a -> p.clears.forEach { a.put(it) } })
            val bosses = JSONObject()
            p.bosses.forEach { (id, b) ->
                bosses.put(id, JSONObject().put("defeated", b.defeated).put("wins", b.wins).put("last_correct", b.lastCorrect))
            }
            o.put("bosses", bosses)
            prefs.edit().putString("progress", o.toString()).apply()
        } catch (e: Exception) {
        }
    }

    fun reset() {
        prefs.edit().clear().apply()
    }
}

/* ------------------------------------------------------------------ */
/* Exam builder (methodology Huawei ICT Competition Computing Track)   */
/*    Preliminary: 60 Q / 60 min  |  National: 150 Q / 120 min         */
/*    openEuler 50% - openGauss 30% - Kunpeng 20%, score /1000         */
/* ------------------------------------------------------------------ */

object ExamBuilder {

    fun buildSubjectMock(phase: String, subject: String, bank: List<ExamQuestion>): BuiltExam? {
        if (bank.isEmpty()) return null
        val random = java.util.Random(System.currentTimeMillis())
        val shuffled = bank.shuffled(random)
        val count = min(30, shuffled.size)
        return BuiltExam(
            id = "mock-$phase-$subject",
            title = "Mock Exam • $subject",
            subtitle = "Format Huawei : QCM complet, sans feedback immédiat, score /1000",
            phase = phase,
            subject = subject,
            questions = shuffled.take(count),
            durationMin = min(40, (count * 1.2).roundToInt().coerceAtLeast(15)),
        )
    }

    fun buildPhaseExam(phase: String, questionCount: Int, durationMin: Int, banks: Map<String, List<ExamQuestion>>): BuiltExam? {
        val available = banks.entries.flatMap { (subject, list) -> list.map { subject to it } }
        if (available.isEmpty()) return null
        val total = if (questionCount < available.size) questionCount else available.size
        val random = java.util.Random(System.currentTimeMillis())
        val selected = mutableListOf<ExamQuestion>()
        val usedPrompts = HashSet<String>()
        var result = 0
        // First pass: fill proportionally to official weights (50/30/20).
        for (subject in SUBJECTS) {
            val weight = SUBJECT_WEIGHTS[subject] ?: 0
            var want = (total * weight) / 100
            val bank = banks[subject] ?: continue
            val shuffled = bank.shuffled(random)
            var taken = 0
            for (eq in shuffled) {
                if (taken >= want) break
                val key = normalizeAnswer(eq.question.prompt)
                if (usedPrompts.contains(key)) continue
                usedPrompts.add(key)
                selected.add(eq)
                taken++
            }
            result += taken
        }
        // Second pass: deal with short banks by topping up in official weight order.
        for (subject in SUBJECTS) {
            if (result >= total) break
            val bank = banks[subject] ?: continue
            for (eq in bank.shuffled(random)) {
                if (result >= total) break
                val key = normalizeAnswer(eq.question.prompt)
                if (usedPrompts.contains(key)) continue
                usedPrompts.add(key)
                selected.add(eq)
                result++
            }
        }
        return BuiltExam(
            id = "phase-$phase",
            title = "Examen de phase • Computing Track $phase",
            subtitle = "Méthodologie Huawei, openEuler 50 % / openGauss 30 % / Kunpeng 20 %",
            phase = phase,
            subject = null,
            questions = selected.shuffled(random),
            durationMin = durationMin,
        )
    }
}

/* ------------------------------------------------------------------ */
/* App repository exposed to the UI via Compose state                  */
/* ------------------------------------------------------------------ */

class AppRepository(context: Context) {
    private val loader = ContentLoader(context)
    private val store = ProgressStore(context)

    var phases: List<Phase> = loader.loadPhases()
        private set
    var progress: Progress by mutableStateOf(store.load())
        private set

    val appContext: Context = context

    val subjectsByPhase: Map<String, List<SubjectIndex>> =
        phases.associate { p -> p.id to loader.allSubjects(p.id) }

    fun phase(id: String): Phase? = phases.firstOrNull { it.id.equals(id, ignoreCase = true) }

    fun subjects(phaseId: String): List<SubjectIndex> =
        subjectsByPhase[phase(phaseId)?.id ?: phaseId] ?: emptyList()

    fun subject(phaseId: String, subjectId: String): SubjectIndex? =
        subjects(phaseId).firstOrNull { it.subject.equals(subjectId, ignoreCase = true) }

    fun meta(phaseId: String, subjectId: String, chapterId: String): ChapterMeta? =
        subject(phaseId, subjectId)?.chapters?.firstOrNull { it.id == chapterId }

    fun chapter(phaseId: String, subjectId: String, chapterId: String): Chapter? {
        val meta = meta(phaseId, subjectId, chapterId) ?: return null
        return loader.chapter(phaseId, subjectId, meta.file)
    }

    fun chapterById(chapterId: String): Chapter? {
        val phaseObj = phase(chapterId.substringBefore('-')) ?: return null
        val subjectGuess = when {
            chapterId.contains("-oe-") -> "openEuler"
            chapterId.contains("-og-") -> "openGauss"
            chapterId.contains("-kp-") -> "Kunpeng"
            else -> null
        } ?: return null
        val sub = subject(phaseObj.id, subjectGuess) ?: return null
        return sub.chapters.firstOrNull { it.id.equals(chapterId, ignoreCase = true) }
            ?.let { loader.chapter(phaseObj.id, subjectGuess, it.file) }
    }

    fun chapterProgress(id: String): ChapterProgress = progress.chapters[id] ?: ChapterProgress()

    fun passedChapters(phaseId: String, subjectId: String): List<ChapterMeta> {
        val sub = subject(phaseId, subjectId) ?: return emptyList()
        return sub.chapters.filter { chapterProgress(it.id).passed }
    }

    fun nextChapter(phaseId: String, subjectId: String): ChapterMeta? {
        val sub = subject(phaseId, subjectId) ?: return null
        return sub.chapters.firstOrNull { !chapterProgress(it.id).passed }
    }

    fun subjectProgress(phaseId: String, subjectId: String): SubjectProgress {
        val sub = subject(phaseId, subjectId)
        val total = sub?.chapters?.size ?: 0
        val passed = if (sub == null) 0 else sub.chapters.count { chapterProgress(it.id).passed }
        val mockKey = "$phaseId|$subjectId"
        val mock = progress.mocks[mockKey]
        return SubjectProgress(
            passedChapters = passed,
            totalChapters = total,
            mockPassed = mock?.passed ?: false,
        )
    }

    fun phaseExams(phaseId: String): List<PhaseResult> = progress.phaseExams.filter { it.phase == phaseId }

    fun phaseReady(phaseId: String): Boolean {
        val subs = subjects(phaseId)
        if (subs.isEmpty()) return false
        return subs.all { s ->
            val sp = subjectProgress(phaseId, s.subject)
            sp.totalChapters > 0 && sp.passedChapters == sp.totalChapters && sp.mockPassed
        }
    }

    fun buildSubjectMockExam(phaseId: String, subjectId: String): BuiltExam? {
        val bank = loader.questionBank(phaseId, subjectId)
        return ExamBuilder.buildSubjectMock(phaseId, subjectId, bank)
    }

    fun buildPhaseExam(phaseId: String): BuiltExam? {
        val phase = phase(phaseId) ?: return null
        val banks = SUBJECTS.associateWith { loader.questionBank(phaseId, it) }.filterValues { it.isNotEmpty() }
        return ExamBuilder.buildPhaseExam(phaseId, phase.questionCount, phase.durationMin, banks)
    }

    private fun today(): String = LocalDate.now().toString()

    private fun addDays(fromIso: String, days: Long): String = try {
        LocalDate.parse(fromIso).plusDays(days).toString()
    } catch (e: Exception) {
        LocalDate.now().plusDays(days).toString()
    }

    private fun dailyKey(subject: String): String = "${today()}|$subject"

    fun dailyDone(subject: String): Boolean = progress.daily[dailyKey(subject)] ?: false

    fun dailyTasks(): List<DailyTask> {
        val phaseId = progress.selectedPhase
        val tasks = mutableListOf<DailyTask>()
        for (subject in SUBJECTS) {
            val sp = subjectProgress(phaseId, subject)
            val done = dailyDone(subject)
            val next = nextChapter(phaseId, subject)
            val task = when {
                next != null -> DailyTask(subject, TaskKind.CHAPTER, next, done, chapterProgress(next.id).bestScore)
                !sp.mockPassed -> DailyTask(subject, TaskKind.MOCK, null, done, progress.mocks["$phaseId|$subject"]?.score)
                else -> DailyTask(subject, TaskKind.DONE, null, true, null)
            }
            tasks.add(task)
        }
        return tasks
    }

    fun markRead(chapterId: String) {
        val cp = chapterProgress(chapterId)
        update { it.copy(chapters = it.chapters + (chapterId to cp.copy(read = true))) }
        touchStreak()
    }

    fun recordQuiz(chapterId: String, subject: String, questionCount: Int, correct: Int) {
        if (questionCount <= 0) return
        val score = (correct * 100) / questionCount
        val cp = chapterProgress(chapterId)
        update {
            it.copy(
                chapters = it.chapters + (chapterId to cp.copy(
                    bestScore = maxOf(cp.bestScore, score),
                    passed = cp.passed || score >= 60,
                    attempts = cp.attempts + 1,
                )),
                totalQuestions = it.totalQuestions + questionCount,
                totalCorrect = it.totalCorrect + correct,
                xp = it.xp + correct * 10 + if (score >= 60) 60 else 0,
            )
        }
        touchStreak()
        if (score >= 60) markDaily(subject)
    }

    fun recordMock(phaseId: String, subject: String, correct: Int, total: Int, score: Int) {
        val mock = MockResult(score = score, passed = score >= 600, date = today(), correct = correct, total = total)
        update {
            it.copy(
                mocks = it.mocks + ("$phaseId|$subject" to mock),
                totalQuestions = it.totalQuestions + total,
                totalCorrect = it.totalCorrect + correct,
                xp = it.xp + correct * 8 + if (score >= 600) 150 else 0,
            )
        }
        touchStreak()
        mock.let { m ->
            if (m.passed) markDaily(subject)
        }
    }

    fun recordPhaseExam(phaseId: String, correct: Int, total: Int, score: Int) {
        val pe = PhaseResult(
            phase = phaseId, score = score, passed = score >= 600,
            date = today(), correct = correct, total = total,
        )
        update {
            it.copy(
                phaseExams = it.phaseExams.filter { e -> !(e.phase == phaseId && e.score < pe.score) } + pe,
                totalQuestions = it.totalQuestions + total,
                totalCorrect = it.totalCorrect + correct,
                xp = it.xp + correct * 8 + if (pe.passed) 250 else 0,
            )
        }
        touchStreak()
    }

    fun markDaily(subject: String) {
        val key = dailyKey(subject)
        if (progress.daily[key] == true) return
        update { it.copy(daily = it.daily + (key to true)) }
        touchStreak()
    }

    fun touchStreak() {
        val today = today()
        val current = progress.lastStudyDay
        if (current == today) return
        val streak = when {
            current == null || current.isBlank() -> 1
            else -> {
                try {
                    val yesterday = LocalDate.now().minusDays(1).toString()
                    if (current == yesterday) progress.streak + 1 else 1
                } catch (e: Exception) {
                    1
                }
            }
        }
        update { it.copy(streak = streak, lastStudyDay = today) }
    }

    // ==================== Coach mémoire (SRS des notions ratées) ====================

    /** Index par sujet|phase → liste de questions (pour le Coach et l'Arène). */
    val questionPool: Map<String, List<Question>> by lazy {
        val m = HashMap<String, MutableList<Question>>()
        phases.forEach { p ->
            subjects(p.id).forEach { s ->
                s.chapters.forEach { fm ->
                    loader.chapter(p.id, s.subject, fm.file)?.quiz?.forEach { q ->
                        if (q.id.isNotBlank()) m.getOrPut("${p.id}|${s.subject}") { mutableListOf() }.add(q)
                    }
                }
            }
        }
        m
    }

    private val questionById: Map<String, Question> by lazy {
        val m = HashMap<String, Question>()
        questionPool.forEach { (_, list) -> list.forEach { q -> m[q.id] = q } }
        m
    }

    fun coachQuestion(id: String): Question? = questionById[id]

    /**
     * Enregistre le résultat d'une question ET tient le SRS :
     * - raté  → carte due aujourd'hui (streak remis à 0)
     * - juste → carte repoussée (grâce croissante), 5 succès consécutifs = maîtrisée
     */
    fun recordQuestionResult(questionId: String, correct: Boolean) {
        if (questionId.isBlank()) return
        update { p ->
            val st = p.questionStats[questionId] ?: QStat()
            val qstat = if (correct) st.copy(correct = st.correct + 1) else st.copy(wrong = st.wrong + 1)
            val cl = p.srs[questionId] ?: SrsCard(due = today(), streak = 0)
            val card = if (correct) {
                if (cl.streak + 1 >= 5) null else cl.copy(streak = cl.streak + 1, due = addDays(today(), ((cl.streak + 1) * 2).toLong()))
            } else {
                SrsCard(due = today(), streak = 0)
            }
            val newSrs: Map<String, SrsCard> = if (card == null) p.srs - questionId else p.srs + (questionId to card)
            val clears = if (card == null) (p.clears + questionId).distinct() else p.clears
            val xpGain = when {
                correct && card == null -> 40 // maîtrisée (5e succès consécutif)
                correct && cl.streak > 0 -> 12  // révision réussie
                correct -> 6
                else -> 2
            }
            p.copy(
                questionStats = p.questionStats + (questionId to qstat),
                srs = newSrs,
                clears = clears,
                xp = p.xp + xpGain,
            )
        }
        touchStreak()
    }

    /** Ids des notions à retravailler aujourd'hui (due <= aujourd'hui), les plus urgentes d'abord. */
    fun coachDueIds(): List<String> {
        val day = today()
        return progress.srs.entries
            .filter { it.key !in progress.clears && it.value.due <= day }
            .sortedBy { it.value.due }
            .map { it.key }
    }

    fun coachCount(): Int = coachDueIds().size

    /** Ajoute manuellement une notion au Coach (après une erreur en quiz). */
    fun enqueueCoach(questionId: String) {
        update { p ->
            if (p.srs.containsKey(questionId)) p
            else p.copy(srs = p.srs + (questionId to SrsCard(due = today(), streak = 0)))
        }
    }

    // ==================== Arène Boss ====================

    /** Round de boss : `count` questions du sujet dans la phase, mélangées. */
    fun bossRounds(phaseId: String, subjectId: String, count: Int = 12): List<Question> {
        val pool = questionPool["$phaseId|$subjectId"].orEmpty().ifEmpty {
            questionPool.values.flatten().filter { it.id.startsWith("hcia") || it.id.startsWith("hcip") || it.id.startsWith("hcie") }
        }
        if (pool.isEmpty()) return emptyList()
        return pool.shuffled().take(count)
    }

    fun recordBoss(phaseId: String, subjectId: String, correct: Int, total: Int, defeated: Boolean) {
        if (total <= 0) return
        val key = "$phaseId|$subjectId"
        update { p ->
            val prev = p.bosses[key] ?: BossResult()
            p.copy(
                bosses = p.bosses + (key to BossResult(
                    defeated = prev.defeated || defeated,
                    wins = prev.wins + if (defeated) 1 else 0,
                    lastCorrect = correct,
                )),
                xp = p.xp + correct * 10 + if (defeated) 200 else 40,
            )
        }
        touchStreak()
        if (defeated) markDaily(subjectId)
    }

    fun bossProgress(): Map<String, BossResult> = progress.bosses

    fun selectPhase(phaseId: String) {
        update { it.copy(selectedPhase = phaseId) }
    }

    fun setOnboarded() {
        update { it.copy(onboarded = true) }
    }

    fun resetProgress() {
        store.reset()
        progress = store.load()
    }

    private fun update(transform: (Progress) -> Progress) {
        val next = transform(progress)
        progress = next
        store.save(next)
        // Relances dynamiques : tant que la journée n'est pas finie, on harcèle.
        try {
            com.klein.ictcomputing.alarm.NagSystem.sync(appContext)
        } catch (_: Exception) {
        }
    }

    fun nextAlarmLabel(): String {
        val hour = AlarmSystem.alarmHour(appContext)
        val minute = AlarmSystem.alarmMinute(appContext)
        return "%02d:%02d".format(hour, minute)
    }

    fun nextAlarmMillis(): Long {
        val prefs = appContext.getSharedPreferences("ict_alarm", Context.MODE_PRIVATE)
        return if (AlarmSystem.enabled(appContext)) prefs.getLong("next", 0L) else 0L
    }

    fun isTodayDone(): Boolean = dailyTasks().all { it.kind == TaskKind.CHAPTER && it.done || it.kind == TaskKind.MOCK && it.done || it.kind == TaskKind.DONE }

    /** Content coverage summary, useful in settings. */
    fun contentStats(): Triple<Int, Int, Int> {
        var chapters = 0
        var questions = 0
        var minutes = 0
        subjectsByPhase.forEach { (_, subs) ->
            subs.forEach { s -> chapters += s.chapters.size }
        }
        SUBJECTS.forEach { subject ->
            subjectsByPhase.forEach { (phase, _) ->
                loader.allChapters(phase, subject).forEach {
                    questions += it.quiz.size
                    minutes += it.minutes
                }
            }
        }
        return Triple(chapters, questions, minutes)
    }

    fun firstPendingTask(): DailyTask? = dailyTasks().firstOrNull { !it.done }

    /** Ouvre automatiquement la session du jour (lancement par l'alarme 03:00). */
    var pendingSession by mutableStateOf(false)
        private set

    fun markPendingSession() {
        pendingSession = true
    }

    fun consumePendingSession(): Boolean {
        val wasPending = pendingSession
        pendingSession = false
        return wasPending
    }

    companion object {
        const val EXTRA_START_SESSION = "com.klein.ictcomputing.OPEN_SESSION"
        const val EXTRA_FROM_ALARM = "com.klein.ictcomputing.FROM_ALARM"
    }
}

/** progression de niveau « arcade » : 500 XP par niveau */
fun levelOf(xp: Int): Int = xp / 500 + 1
fun xpInLevel(xp: Int): Int = xp % 500

fun nowHourMinute(): IntArray {
    val cal = Calendar.getInstance()
    return intArrayOf(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}
