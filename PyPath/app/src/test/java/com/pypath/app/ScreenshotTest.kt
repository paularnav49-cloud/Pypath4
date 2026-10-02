package com.pypath.app

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.pypath.app.data.model.StepType
import com.pypath.app.data.progress.QuizResult
import com.pypath.app.data.progress.SubLevelProgress
import com.pypath.app.domain.CourseSnapshot
import com.pypath.app.ui.mcq.McqQuestionCard
import com.pypath.app.ui.mcq.McqSession
import com.pypath.app.ui.screens.completion.CompletionScreen
import com.pypath.app.ui.screens.dashboard.DashboardScreen
import com.pypath.app.ui.screens.lesson.LessonScreen
import com.pypath.app.ui.screens.level.LevelScreen
import com.pypath.app.ui.screens.onboarding.OnboardingScreen
import com.pypath.app.ui.screens.sublevel.SubLevelScreen
import com.pypath.app.ui.theme.AppTheme
import com.pypath.app.ui.theme.PyPathTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/** Renders key screens to PNG for visual review (./gradlew recordPaparazziDebug). */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, maxPercentDifference = 0.1)

    private val course = loadTestCourse()
    private val midProgress = completed("l1s1").let {
        it.copy(
            currentSubLevelId = "l1s2",
            subLevels = it.subLevels + ("l1s2" to SubLevelProgress(completedSteps = setOf(StepType.LEARN), quiz = QuizResult(1, 1, 4, 1, false))),
        )
    }
    private val snap = CourseSnapshot(course, midProgress)

    private fun shot(dark: Boolean = false, content: @Composable () -> Unit) {
        if (dark) paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_6.copy(nightMode = NightMode.NIGHT))
        paparazzi.snapshot { PyPathTheme(dark = dark) { content() } }
    }

    @Test fun onboarding() = shot { OnboardingScreen(onFinish = {}) }
    @Test fun dashboardFresh() = shot { DashboardScreen(CourseSnapshot(course, completed()), {}, {}, {}) }
    @Test fun dashboard() = shot { DashboardScreen(snap, {}, {}, {}) }
    @Test fun dashboardDark() = shot(dark = true) { DashboardScreen(snap, {}, {}, {}) }
    @Test fun level() = shot { LevelScreen(snap, "l1", {}, {}) }
    @Test fun subLevel() = shot { SubLevelScreen(snap, "l1s2", {}, {}, {}, {}) }
    @Test fun lesson() = shot { LessonScreen(snap.subLevel("l1s2"), {}, {}) }
    @Test fun completion() = shot { CompletionScreen(CourseSnapshot(course, completed("l1s1", "l1s2")), "l1s2", {}, {}, {}) }

    @Test fun mcqFeedbackWrong() = shot {
        val q = snap.subLevel("l1s2")!!.subLevel.quiz!!
        val s = McqSession(q.questions, q.passPercent).apply { select(0); check(); next(); select(0); check() }
        Column(Modifier.fillMaxSize().background(AppTheme.colors.background).padding(20.dp)) { McqQuestionCard(s) }
    }

    @Test fun mcqSelected() = shot {
        val q = snap.subLevel("l1s1")!!.subLevel.quiz!!
        val s = McqSession(q.questions, q.passPercent).apply { select(1) }
        Column(Modifier.fillMaxSize().background(AppTheme.colors.background).padding(20.dp)) { McqQuestionCard(s) }
    }
}
