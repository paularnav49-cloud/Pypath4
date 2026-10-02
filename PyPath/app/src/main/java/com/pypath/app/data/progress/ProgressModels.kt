package com.pypath.app.data.progress

import com.pypath.app.data.model.StepType
import kotlinx.serialization.Serializable

/**
 * Everything the app remembers about the learner. Stored locally as JSON.
 * Add new fields with default values so older saved data keeps decoding.
 */
@Serializable
data class UserProgress(
    val schemaVersion: Int = 1,
    val onboardingCompleted: Boolean = false,
    /** Sub-level the learner is currently working on (used by "Continue Learning"). */
    val currentSubLevelId: String? = null,
    val subLevels: Map<String, SubLevelProgress> = emptyMap(),
) {
    fun of(subLevelId: String): SubLevelProgress = subLevels[subLevelId] ?: SubLevelProgress()
}

@Serializable
data class SubLevelProgress(
    val completedSteps: Set<StepType> = emptySet(),
    val quiz: QuizResult? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val lastOpenedAt: Long? = null,
)

@Serializable
data class QuizResult(
    val lastScore: Int,
    val bestScore: Int,
    val total: Int,
    val attempts: Int,
    val passed: Boolean,
)
