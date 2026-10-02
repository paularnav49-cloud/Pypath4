package com.pypath.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Course content model.
 *
 * All learning content is data-driven (loaded from assets/course/course.json) so new
 * levels, sub-levels, lessons and questions can be added without touching UI code.
 *
 * Hierarchy:  Course → Level → SubLevel → Steps (Learn, Quiz, [Practice], ...)
 */
@Serializable
data class Course(
    val id: String,
    val title: String,
    val schemaVersion: Int = 1,
    val levels: List<Level>,
)

@Serializable
data class Level(
    val id: String,
    val number: Int,
    val title: String,
    val description: String,
    /** When false the level is shown as "Coming soon" and can never be unlocked. */
    val available: Boolean = true,
    val subLevels: List<SubLevel>,
    /**
     * Reserved for future level-final projects / assessments. Kept nullable so
     * existing content files stay valid when the feature ships.
     */
    val finalAssessment: AssessmentRef? = null,
)

@Serializable
data class AssessmentRef(
    val id: String,
    val type: String,
)

@Serializable
data class SubLevel(
    val id: String,
    /** Display code such as "1.2". */
    val code: String,
    val title: String,
    val summary: String,
    val estimatedMinutes: Int = 5,
    /**
     * Ordered list of steps the learner must finish to complete the sub-level.
     * Defaults to LEARN → QUIZ. Future content can add PRACTICE, PROJECT, etc.
     */
    val steps: List<StepType> = listOf(StepType.LEARN, StepType.QUIZ),
    val lesson: Lesson? = null,
    val quiz: Quiz? = null,
)

@Serializable
enum class StepType {
    @SerialName("learn") LEARN,
    @SerialName("quiz") QUIZ,
    @SerialName("practice") PRACTICE,
    @SerialName("project") PROJECT;

    /** Steps the current build can actually run. Others render as "Coming soon". */
    val isSupported: Boolean get() = this == LEARN || this == QUIZ

    val label: String
        get() = when (this) {
            LEARN -> "Learn"
            QUIZ -> "MCQs"
            PRACTICE -> "Practice"
            PROJECT -> "Project"
        }
}

// ─────────────────────────── Lesson ───────────────────────────

/** A lesson is split into short pages so learners never face a wall of text. */
@Serializable
data class Lesson(
    val pages: List<LessonPage>,
)

@Serializable
data class LessonPage(
    val title: String,
    val blocks: List<LessonBlock>,
)

@Serializable
sealed interface LessonBlock {
    @Serializable @SerialName("text")
    data class Text(val text: String) : LessonBlock

    @Serializable @SerialName("bullets")
    data class Bullets(val items: List<String>) : LessonBlock

    @Serializable @SerialName("code")
    data class Code(
        val code: String,
        val output: String? = null,
        /** Line-by-line or overall plain-language explanation of the code. */
        val explanation: List<String> = emptyList(),
        val caption: String? = null,
    ) : LessonBlock

    @Serializable @SerialName("tip")
    data class Tip(val title: String = "Tip", val text: String) : LessonBlock

    @Serializable @SerialName("keypoint")
    data class KeyPoint(val text: String) : LessonBlock
}

// ─────────────────────────── Quiz / MCQ ───────────────────────────

@Serializable
data class Quiz(
    val questions: List<Question>,
    /** Minimum percentage of correct answers required to complete the sub-level. */
    val passPercent: Int = 60,
)

@Serializable
data class Question(
    val id: String,
    val prompt: String,
    /** Optional code shown with the question (e.g. "What does this print?"). */
    val code: String? = null,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)
