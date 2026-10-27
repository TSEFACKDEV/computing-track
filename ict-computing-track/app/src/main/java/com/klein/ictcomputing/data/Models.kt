package com.klein.ictcomputing.data

enum class QType { SINGLE, MULTI, TF, TEXT }

data class Phase(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val durationMin: Int,
    val questionCount: Int,
)

data class SubjectIndex(
    val phase: String,
    val subject: String,
    val name: String,
    val tagline: String,
    val weight: Int,
    val chapters: List<ChapterMeta>,
)

data class ChapterMeta(
    val id: String,
    val file: String,
    val title: String,
    val minutes: Int,
)

data class Chapter(
    val id: String,
    val phase: String,
    val subject: String,
    val number: Int,
    val titleFr: String,
    val titleEn: String,
    val minutes: Int,
    val source: String,
    val objectives: List<String>,
    val sections: List<CourseSection>,
    val keyPoints: List<String>,
    val traps: List<String>,
    val quiz: List<Question>,
)

data class CourseSection(
    val heading: String,
    val body: String,
    val bullets: List<String>,
    val code: List<String>,
    val table: CourseTable?,
    val tip: String?,
    /** Schéma ASCII / textuel affiché dans un bloc monospace. */
    val diagram: String? = null,
    /** Exercices corrigés intégrés à la section. */
    val exercises: List<CourseExercise> = emptyList(),
)

data class CourseExercise(
    val prompt: String,
    val correction: String,
)

data class CourseTable(
    val headers: List<String>,
    val rows: List<List<String>>,
)

data class Question(
    val id: String,
    val type: QType,
    val prompt: String,
    val options: List<String>,
    val answer: List<Int>,
    val accept: List<String>,
    val sampleAnswer: String?,
    val explanationFr: String,
    val topic: String,
)

data class ChapterProgress(
    val read: Boolean = false,
    val bestScore: Int = 0,
    val passed: Boolean = false,
    val attempts: Int = 0,
)

data class MockResult(
    val score: Int,
    val passed: Boolean,
    val date: String,
    val correct: Int,
    val total: Int,
)

data class PhaseResult(
    val phase: String,
    val score: Int,
    val passed: Boolean,
    val date: String,
    val correct: Int,
    val total: Int,
)

data class Progress(
    val selectedPhase: String = "HCIA",
    val onboarded: Boolean = false,
    val chapters: Map<String, ChapterProgress> = emptyMap(),
    val daily: Map<String, Boolean> = emptyMap(),
    val streak: Int = 0,
    val lastStudyDay: String? = null,
    val mocks: Map<String, MockResult> = emptyMap(),
    val phaseExams: List<PhaseResult> = emptyList(),
    val totalQuestions: Int = 0,
    val totalCorrect: Int = 0,
    val xp: Int = 0,
    /* --- Coach mémoire (rattrapage des notions ratées) --- */
    val questionStats: Map<String, QStat> = emptyMap(),
    val srs: Map<String, SrsCard> = emptyMap(),
    val clears: List<String> = emptyList(),
    /* --- Arène Boss --- */
    val bosses: Map<String, BossResult> = emptyMap(),
)

/** Statistiques par question : feedback pour le coach mémoire. */
data class QStat(
    val correct: Int = 0,
    val wrong: Int = 0,
)

/** Carte de rattrapage : due = jour (ISO) où la notion doit ressurgir. */
data class SrsCard(
    val due: String,
    val streak: Int = 0,
)

/** Résultat d'un combat de boss par phase|subject. */
data class BossResult(
    val defeated: Boolean = false,
    val wins: Int = 0,
    val lastCorrect: Int = 0,
)

data class DailyTask(
    val subject: String,
    val kind: TaskKind,
    val chapter: ChapterMeta?,
    val done: Boolean,
    val score: Int?,
)

enum class TaskKind { CHAPTER, MOCK, PHASE_EXAM, DONE }

data class SubjectProgress(
    val passedChapters: Int,
    val totalChapters: Int,
    val mockPassed: Boolean,
) {
    val percent: Int
        get() = if (totalChapters == 0) 0 else (passedChapters * 100) / totalChapters
}

data class ExamQuestion(
    val question: Question,
    val subject: String,
    val chapterId: String,
)

data class BuiltExam(
    val id: String,
    val title: String,
    val subtitle: String,
    val phase: String,
    val subject: String?,
    val questions: List<ExamQuestion>,
    val durationMin: Int,
)
