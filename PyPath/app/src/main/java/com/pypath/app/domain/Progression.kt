package com.pypath.app.domain

import com.pypath.app.data.model.Course
import com.pypath.app.data.model.Level
import com.pypath.app.data.model.SubLevel
import com.pypath.app.data.progress.SubLevelProgress
import com.pypath.app.data.progress.UserProgress

enum class NodeStatus { LOCKED, AVAILABLE, IN_PROGRESS, COMPLETED, COMING_SOON }

data class SubLevelState(
    val level: Level,
    val subLevel: SubLevel,
    val indexInLevel: Int,
    val status: NodeStatus,
    val progress: SubLevelProgress,
    /** The sub-level the learner must finish before this one unlocks (for messaging). */
    val unlockedBy: SubLevel?,
) {
    val isPlayable get() = status == NodeStatus.AVAILABLE || status == NodeStatus.IN_PROGRESS || status == NodeStatus.COMPLETED
}

data class LevelState(
    val level: Level,
    val status: NodeStatus,
    val subLevels: List<SubLevelState>,
) {
    val completedCount get() = subLevels.count { it.status == NodeStatus.COMPLETED }
    val total get() = subLevels.size
    val fraction get() = if (total == 0) 0f else completedCount.toFloat() / total
}

/**
 * Pure, testable progression rules. The single place that decides what is unlocked.
 *
 * Current rule: sub-levels unlock linearly across the whole course — a sub-level is
 * available once the previous sub-level (in course order) is completed. A level is
 * unlocked when its first sub-level is. Future rules (e.g. level-final assessment gates)
 * plug in here without touching the UI.
 */
class CourseSnapshot(val course: Course, val progress: UserProgress) {

    val levels: List<LevelState>
    private val byId: Map<String, SubLevelState>
    val orderedSubLevels: List<SubLevelState>

    init {
        val result = mutableListOf<LevelState>()
        var previousCompleted = true
        var previous: SubLevel? = null
        for (level in course.levels) {
            val subs = level.subLevels.mapIndexed { idx, sub ->
                val p = progress.of(sub.id)
                val status = when {
                    !level.available -> NodeStatus.COMING_SOON
                    p.completed -> NodeStatus.COMPLETED
                    !previousCompleted -> NodeStatus.LOCKED
                    p.completedSteps.isNotEmpty() || p.quiz != null -> NodeStatus.IN_PROGRESS
                    else -> NodeStatus.AVAILABLE
                }
                val state = SubLevelState(level, sub, idx, status, p, previous)
                previousCompleted = level.available && p.completed
                previous = sub
                state
            }
            val levelStatus = when {
                !level.available -> NodeStatus.COMING_SOON
                subs.isNotEmpty() && subs.all { it.status == NodeStatus.COMPLETED } -> NodeStatus.COMPLETED
                subs.any { it.status == NodeStatus.COMPLETED || it.status == NodeStatus.IN_PROGRESS } -> NodeStatus.IN_PROGRESS
                subs.firstOrNull()?.status == NodeStatus.AVAILABLE -> NodeStatus.AVAILABLE
                else -> NodeStatus.LOCKED
            }
            result += LevelState(level, levelStatus, subs)
        }
        levels = result
        orderedSubLevels = result.flatMap { it.subLevels }
        byId = orderedSubLevels.associateBy { it.subLevel.id }
    }

    fun subLevel(id: String): SubLevelState? = byId[id]
    fun level(id: String): LevelState? = levels.firstOrNull { it.level.id == id }

    /** Sub-level that "Continue Learning" should open, or null when everything available is done. */
    val continueTarget: SubLevelState?
        get() {
            val saved = progress.currentSubLevelId?.let { byId[it] }
            if (saved != null && saved.isPlayable && saved.status != NodeStatus.COMPLETED) return saved
            return orderedSubLevels.firstOrNull {
                it.status == NodeStatus.AVAILABLE || it.status == NodeStatus.IN_PROGRESS
            }
        }

    /** The level/sub-level shown as "current" on the dashboard. */
    val current: SubLevelState?
        get() = continueTarget ?: orderedSubLevels.lastOrNull { it.status == NodeStatus.COMPLETED }

    fun next(after: String): SubLevelState? {
        val idx = orderedSubLevels.indexOfFirst { it.subLevel.id == after }
        return orderedSubLevels.getOrNull(idx + 1)
    }

    val playableTotal get() = orderedSubLevels.count { it.status != NodeStatus.COMING_SOON }
    val completedTotal get() = orderedSubLevels.count { it.status == NodeStatus.COMPLETED }
    val overallFraction get() = if (playableTotal == 0) 0f else completedTotal.toFloat() / playableTotal
    val allAvailableComplete get() = playableTotal > 0 && completedTotal == playableTotal
}
