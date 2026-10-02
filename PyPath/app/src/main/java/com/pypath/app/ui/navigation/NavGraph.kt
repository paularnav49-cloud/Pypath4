package com.pypath.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pypath.app.domain.CourseSnapshot
import com.pypath.app.ui.AppState
import com.pypath.app.ui.AppViewModel
import com.pypath.app.ui.screens.completion.CompletionScreen
import com.pypath.app.ui.screens.dashboard.DashboardScreen
import com.pypath.app.ui.screens.lesson.LessonScreen
import com.pypath.app.ui.screens.level.LevelScreen
import com.pypath.app.ui.screens.onboarding.OnboardingScreen
import com.pypath.app.ui.screens.quiz.QuizScreen
import com.pypath.app.ui.screens.sublevel.SubLevelScreen
import com.pypath.app.ui.theme.AppTheme

/** All destinations in one place. Future: practice/{id}, project/{levelId}, settings, ... */
object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val LEVEL = "level/{id}"
    const val SUBLEVEL = "sublevel/{id}"
    const val LESSON = "lesson/{id}"
    const val QUIZ = "quiz/{id}"
    const val COMPLETE = "complete/{id}"

    fun level(id: String) = "level/$id"
    fun subLevel(id: String) = "sublevel/$id"
    fun lesson(id: String) = "lesson/$id"
    fun quiz(id: String) = "quiz/$id"
    fun complete(id: String) = "complete/$id"
}

@Composable
fun PyPathNavHost(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val c = AppTheme.colors
    when (val s = state) {
        AppState.Loading -> Box(Modifier.fillMaxSize().background(c.background))
        is AppState.Error -> Box(Modifier.fillMaxSize().background(c.background).padding(24.dp), contentAlignment = Alignment.Center) {
            Text(s.message, color = c.error)
        }
        is AppState.Ready -> {
            val nav = rememberNavController()
            // Decided once: changing the start destination later would reset the back stack.
            val start = remember { if (s.snapshot.progress.onboardingCompleted) Routes.DASHBOARD else Routes.ONBOARDING }
            AppNavHost(nav, start, s.snapshot, vm)
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, start: String, snap: CourseSnapshot, vm: AppViewModel) {
    val c = AppTheme.colors
    val dur = 320
    NavHost(
        navController = nav,
        startDestination = start,
        modifier = Modifier.fillMaxSize().background(c.background),
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(dur), initialOffset = { it / 5 }) + fadeIn(tween(dur)) },
        exitTransition = { fadeOut(tween(dur / 2)) },
        popEnterTransition = { fadeIn(tween(dur)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(dur), targetOffset = { it / 5 }) + fadeOut(tween(dur / 2)) },
    ) {
        composable(Routes.ONBOARDING, enterTransition = { fadeIn() }) {
            OnboardingScreen(onFinish = {
                vm.completeOnboarding()
                nav.navigate(Routes.DASHBOARD) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }

        composable(Routes.DASHBOARD, enterTransition = { fadeIn(tween(dur)) }) {
            DashboardScreen(
                snapshot = snap,
                onContinue = { id -> nav.navigate(Routes.subLevel(id)) },
                onOpenLevel = { id -> nav.navigate(Routes.level(id)) },
                onResetProgress = vm::resetProgress,
            )
        }

        composable(Routes.LEVEL) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            LevelScreen(
                snapshot = snap, levelId = id,
                onBack = { nav.popBackStack() },
                onOpenSubLevel = { subId -> nav.navigate(Routes.subLevel(subId)) },
            )
        }

        composable(Routes.SUBLEVEL) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            SubLevelScreen(
                snapshot = snap, subLevelId = id,
                onBack = { nav.popBackStack() },
                onOpened = { vm.openSubLevel(id) },
                onStartLesson = { nav.navigate(Routes.lesson(id)) },
                onStartQuiz = { nav.navigate(Routes.quiz(id)) },
            )
        }

        composable(Routes.LESSON) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val state = snap.subLevel(id)
            LessonScreen(
                state = state,
                onClose = { nav.popBackStack() },
                onFinished = {
                    if (state != null) vm.completeLesson(id, state.subLevel.steps)
                    nav.navigate(Routes.quiz(id)) { popUpTo(Routes.LESSON) { inclusive = true } }
                },
            )
        }

        composable(Routes.QUIZ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val state = snap.subLevel(id)
            QuizScreen(
                state = state,
                onClose = { nav.popBackStack() },
                onSubmitResult = { score, total, passed ->
                    if (state != null) vm.recordQuiz(id, score, total, passed, state.subLevel.steps)
                },
                onPassedContinue = {
                    nav.navigate(Routes.complete(id)) { popUpTo(Routes.SUBLEVEL) { inclusive = true } }
                },
                onReviewLesson = {
                    nav.navigate(Routes.lesson(id)) { popUpTo(Routes.QUIZ) { inclusive = true } }
                },
            )
        }

        composable(Routes.COMPLETE) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            CompletionScreen(
                snapshot = snap, subLevelId = id,
                onNext = { nextId ->
                    nav.navigate(Routes.subLevel(nextId)) { popUpTo(Routes.COMPLETE) { inclusive = true } }
                },
                onBackToLevel = { levelId ->
                    nav.navigate(Routes.level(levelId)) { popUpTo(Routes.DASHBOARD) }
                },
                onDashboard = { nav.navigate(Routes.DASHBOARD) { popUpTo(Routes.DASHBOARD) { inclusive = true } } },
            )
        }
    }
}
