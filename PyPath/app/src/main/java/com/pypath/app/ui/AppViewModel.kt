package com.pypath.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pypath.app.PyPathApp
import com.pypath.app.data.content.CourseRepository
import com.pypath.app.data.model.StepType
import com.pypath.app.data.progress.ProgressRepository
import com.pypath.app.domain.CourseSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppState {
    data object Loading : AppState
    data class Ready(val snapshot: CourseSnapshot) : AppState
    data class Error(val message: String) : AppState
}

/**
 * Activity-scoped view model shared by every screen. It combines static course content
 * with live learner progress into a single [CourseSnapshot].
 */
class AppViewModel(
    private val courses: CourseRepository,
    private val progressRepo: ProgressRepository,
) : ViewModel() {

    val state: StateFlow<AppState> =
        combine(flow { emit(runCatching { courses.loadCourse() }) }, progressRepo.progress) { course, progress ->
            course.fold(
                onSuccess = { AppState.Ready(CourseSnapshot(it, progress)) },
                onFailure = { AppState.Error(it.message ?: "Could not load course") },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)

    fun completeOnboarding() = viewModelScope.launch { progressRepo.completeOnboarding() }

    fun openSubLevel(id: String) = viewModelScope.launch { progressRepo.setCurrentSubLevel(id) }

    fun completeLesson(id: String, steps: List<StepType>) =
        viewModelScope.launch { progressRepo.markStepCompleted(id, StepType.LEARN, steps) }

    fun recordQuiz(id: String, score: Int, total: Int, passed: Boolean, steps: List<StepType>) =
        viewModelScope.launch { progressRepo.recordQuizResult(id, score, total, passed, steps) }

    fun resetProgress() = viewModelScope.launch { progressRepo.reset() }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PyPathApp
                AppViewModel(app.container.courseRepository, app.container.progressRepository)
            }
        }
    }
}
