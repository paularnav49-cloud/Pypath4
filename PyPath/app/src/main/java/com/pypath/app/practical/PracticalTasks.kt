package com.pypath.app.practical

import android.content.Context
import com.pypath.app.domain.CourseSnapshot
import com.pypath.app.domain.NodeStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Hands-on coding tasks for the Practical tab, loaded from `assets/practical/tasks.json`.
 *
 * Each task names the sub-level that teaches its last required concept ([afterSubLevel]).
 * A task unlocks only once that sub-level is completed, so tasks never require concepts the
 * learner hasn't been taught yet. A task with no [afterSubLevel] (the playground) is always open.
 */
@Serializable
data class PracticalTask(
    val id: String,
    val title: String,
    val afterSubLevel: String? = null,
    val concepts: List<String> = emptyList(),
    val instructions: String,
    val requirements: List<String> = emptyList(),
    /** Example interaction shown to explain the goal. Not used to judge the learner's output. */
    val example: String? = null,
    val hint: String? = null,
    val starterCode: String = "",
)

@Serializable
data class PracticalCatalog(val schemaVersion: Int = 1, val tasks: List<PracticalTask>)

object PracticalCatalogParser {
    private val json = Json { ignoreUnknownKeys = true }
    fun parse(text: String): PracticalCatalog = json.decodeFromString(PracticalCatalog.serializer(), text)
}

fun loadPracticalCatalog(context: Context, path: String = "practical/tasks.json"): PracticalCatalog =
    context.assets.open(path).bufferedReader().use { PracticalCatalogParser.parse(it.readText()) }

/** Lock state of a task for the current learner. */
data class TaskAvailability(
    val task: PracticalTask,
    val unlocked: Boolean,
    /** e.g. "1.4 Input and Output" — the sub-level that unlocks it. */
    val requirementLabel: String?,
    /** True when the unlocking sub-level belongs to a level that isn't released yet. */
    val comingSoon: Boolean,
)

fun availability(tasks: List<PracticalTask>, snapshot: CourseSnapshot): List<TaskAvailability> = tasks.map { t ->
    val req = t.afterSubLevel?.let { snapshot.subLevel(it) }
    TaskAvailability(
        task = t,
        unlocked = t.afterSubLevel == null || req?.status == NodeStatus.COMPLETED,
        requirementLabel = req?.let { "${it.subLevel.code} ${it.subLevel.title}" },
        comingSoon = req?.status == NodeStatus.COMING_SOON,
    )
}
