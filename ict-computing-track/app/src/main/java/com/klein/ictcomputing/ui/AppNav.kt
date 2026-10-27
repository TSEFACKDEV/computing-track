package com.klein.ictcomputing.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.klein.ictcomputing.IctApp
import com.klein.ictcomputing.alarm.AlarmSystem
import com.klein.ictcomputing.data.BuiltExam
import com.klein.ictcomputing.data.DailyTask
import com.klein.ictcomputing.data.TaskKind
import com.klein.ictcomputing.focus.FocusMode

sealed interface Route {
    data object Home : Route
    data object Revise : Route
    data object Stats : Route
    data object Settings : Route
    data class Subject(val phaseId: String, val subjectId: String) : Route
    data class Course(val chapterId: String) : Route
    data class Quiz(val chapterId: String) : Route
    data class Exam(val exam: BuiltExam) : Route
    data object Weak : Route
    data class Boss(val phaseId: String, val subjectId: String) : Route
}

private class NavState(initial: Route) {
    private val stack = androidx.compose.runtime.mutableStateListOf<Route>(initial)

    fun current(): Route = stack.last()
    fun push(route: Route) {
        if (stack.lastOrNull() != route) stack.add(route)
    }

    fun pop() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun reset(route: Route) {
        stack.clear()
        stack.add(route)
    }

    val canPop: Boolean get() = stack.size > 1
}

@Composable
fun AppRoot(app: IctApp, openSessionAfterAlarm: Boolean) {
    val repo = app.repo
    val nav = remember { NavState(Route.Home) }

    androidx.compose.runtime.CompositionLocalProvider(LocalApp provides app) {
        // Le mode concentration bloque le Retour tant que le chapitre n'est pas validé.
        BackHandler(enabled = nav.canPop && !FocusMode.active) { nav.pop() }

        LaunchedEffect(openSessionAfterAlarm) {
            if (openSessionAfterAlarm) {
                openTodaySession(repo)?.let { nav.push(it) }
            }
        }

        IctTheme {
            val screen = nav.current()
            if (!repo.progress.onboarded) {
                OnboardingScreen(
                    selectedPhase = repo.progress.selectedPhase,
                    onSelectPhase = { repo.selectPhase(it) },
                    onFinish = {
                        AlarmSystem.schedule(repo.appContext)
                        repo.setOnboarded()
                    },
                )
            } else if (screen is Route.Home || screen is Route.Revise || screen is Route.Stats || screen is Route.Settings) {
                Scaffold(
                    containerColor = ContestColors.Background,
                    bottomBar = {
                        NavigationBar(containerColor = ContestColors.Surface) {
                            TabsBar.entries.forEach { tab ->
                                val selected = containsRoute(screen, tab.route)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { nav.reset(tab.route) },
                                    icon = { Icon(tab.icon, null) },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        when (screen) {
                            is Route.Home -> HomeScreen(
                                onOpenTask = { task, phaseId -> openRoute(repo, task, phaseId)?.let { nav.push(it) } },
                                onOpenSubject = { phase, subject -> nav.push(Route.Subject(phase, subject)) },
                                onTestAlarm = { openAlarmPreview() },
                                onOpenCoach = { nav.push(Route.Weak) },
                            )
                            is Route.Revise -> ReviseScreen(
                                onOpenSubject = { phase, subject -> nav.push(Route.Subject(phase, subject)) },
                                onOpenPhaseExam = { exam -> exam?.let { nav.push(Route.Exam(it)) } },
                            )
                            is Route.Stats -> StatsScreen(
                                onOpenBoss = { subject -> nav.push(Route.Boss(repo.progress.selectedPhase, subject)) },
                            )
                            else -> SettingsScreen(
                                onTestAlarm = { openAlarmPreview() },
                            )
                        }
                    }
                }
            } else {
                when (screen) {
                    is Route.Subject -> SubjectScreen(
                        phaseId = screen.phaseId,
                        subjectId = screen.subjectId,
                        onBack = { nav.pop() },
                        onOpenChapter = { chapterId -> nav.push(Route.Course(chapterId)) },
                        onOpenMock = { exam -> exam?.let { nav.push(Route.Exam(it)) } },
                    )
                    is Route.Course -> CourseScreen(
                        chapterId = screen.chapterId,
                        onBack = { nav.pop() },
                        onTakeQuiz = { chapterId -> nav.push(Route.Quiz(chapterId)) },
                    )
                    is Route.Quiz -> QuizScreen(
                        chapterId = screen.chapterId,
                        onBack = { nav.pop() },
                        onNextAfterPass = { next: Route? -> if (next != null) nav.push(next) else nav.pop() },
                    )
                    is Route.Exam -> ExamScreen(
                        exam = screen.exam,
                        onBack = { nav.pop() },
                    )
                    is Route.Weak -> WeakScreen(onBack = { nav.pop() })
                    is Route.Boss -> BossScreen(
                        phaseId = screen.phaseId,
                        subjectId = screen.subjectId,
                        onBack = { nav.pop() },
                    )
                    else -> Unit
                }
            }
        }
    }
}

private enum class TabsBar(val route: Route, val label: String, val icon: ImageVector) {
    Home(Route.Home, "Seance", Icons.Filled.Home),
    Revise(Route.Revise, "Programme", Icons.Filled.List),
    Stats(Route.Stats, "Progres", Icons.Filled.Star),
    Settings(Route.Settings, "Reglages", Icons.Filled.Settings),
}

private fun containsRoute(current: Route, target: Route): Boolean = when (target) {
    is Route.Home -> current is Route.Home
    is Route.Revise -> current is Route.Revise
    is Route.Stats -> current is Route.Stats
    is Route.Settings -> current is Route.Settings
    else -> false
}

private fun Route.pushTop(route: Route) = route

private fun openTodaySession(repo: com.klein.ictcomputing.data.AppRepository): Route? {
    val task = repo.firstPendingTask()
    val phaseId = repo.progress.selectedPhase
    return openRoute(repo, task, phaseId)
}

internal fun openRoute(
    repo: com.klein.ictcomputing.data.AppRepository,
    task: DailyTask?,
    phaseId: String,
): Route? {
    val t = task ?: return null
    return when (t.kind) {
        TaskKind.CHAPTER -> t.chapter?.let { Route.Course(it.id) }
        TaskKind.MOCK -> repo.buildSubjectMockExam(phaseId, t.subject)?.let { Route.Exam(it) }
        TaskKind.PHASE_EXAM -> repo.buildPhaseExam(phaseId)?.let { Route.Exam(it) }
        TaskKind.DONE -> null
    }
}

internal fun openAlarmPreview() {
    val activityThread = try {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val current = activityThreadClass.getMethod("currentApplication").invoke(null)
        current as android.content.Context
    } catch (e: Exception) {
        null
    } ?: return
    com.klein.ictcomputing.alarm.AlarmActivity.launch(activityThread)
}
