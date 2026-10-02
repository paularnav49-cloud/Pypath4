package com.pypath.app.data.progress

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pypath.app.data.model.StepType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.progressStore: DataStore<Preferences> by preferencesDataStore(name = "pypath_progress")

/** Abstraction so a cloud-synced implementation can be swapped in later. */
interface ProgressRepository {
    val progress: Flow<UserProgress>
    suspend fun completeOnboarding()
    suspend fun setCurrentSubLevel(subLevelId: String)
    suspend fun markStepCompleted(subLevelId: String, step: StepType, requiredSteps: List<StepType>)
    suspend fun recordQuizResult(subLevelId: String, score: Int, total: Int, passed: Boolean, requiredSteps: List<StepType>)
    suspend fun reset()
}

class LocalProgressRepository(private val context: Context) : ProgressRepository {

    private val key = stringPreferencesKey("user_progress_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override val progress: Flow<UserProgress> = context.progressStore.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString(UserProgress.serializer(), it) }.getOrNull() }
            ?: UserProgress()
    }

    private suspend fun update(transform: (UserProgress) -> UserProgress) {
        context.progressStore.edit { prefs ->
            val current = prefs[key]?.let {
                runCatching { json.decodeFromString(UserProgress.serializer(), it) }.getOrNull()
            } ?: UserProgress()
            prefs[key] = json.encodeToString(UserProgress.serializer(), transform(current))
        }
    }

    private fun UserProgress.updateSub(id: String, block: (SubLevelProgress) -> SubLevelProgress) =
        copy(subLevels = subLevels + (id to block(of(id))))

    private fun SubLevelProgress.withCompletionCheck(required: List<StepType>): SubLevelProgress {
        val done = !completed && completedSteps.containsAll(required)
        return if (done) copy(completed = true, completedAt = System.currentTimeMillis()) else this
    }

    override suspend fun completeOnboarding() = update { it.copy(onboardingCompleted = true) }

    override suspend fun setCurrentSubLevel(subLevelId: String) = update {
        it.copy(currentSubLevelId = subLevelId)
            .updateSub(subLevelId) { s -> s.copy(lastOpenedAt = System.currentTimeMillis()) }
    }

    override suspend fun markStepCompleted(subLevelId: String, step: StepType, requiredSteps: List<StepType>) =
        update {
            it.updateSub(subLevelId) { s ->
                s.copy(completedSteps = s.completedSteps + step).withCompletionCheck(requiredSteps)
            }
        }

    override suspend fun recordQuizResult(
        subLevelId: String, score: Int, total: Int, passed: Boolean, requiredSteps: List<StepType>,
    ) = update {
        it.updateSub(subLevelId) { s ->
            val prev = s.quiz
            val result = QuizResult(
                lastScore = score,
                bestScore = maxOf(score, prev?.bestScore ?: 0),
                total = total,
                attempts = (prev?.attempts ?: 0) + 1,
                passed = passed || (prev?.passed ?: false),
            )
            val steps = if (result.passed) s.completedSteps + StepType.QUIZ else s.completedSteps
            s.copy(quiz = result, completedSteps = steps).withCompletionCheck(requiredSteps)
        }
    }

    /** Clears course progress but keeps onboarding as seen. */
    override suspend fun reset() = update { UserProgress(onboardingCompleted = it.onboardingCompleted) }
}
