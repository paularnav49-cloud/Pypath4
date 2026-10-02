package com.pypath.app.ui.screens.sublevel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pypath.app.data.model.StepType
import com.pypath.app.domain.CourseSnapshot
import com.pypath.app.domain.NodeStatus
import com.pypath.app.ui.components.AppButton
import com.pypath.app.ui.components.AppCard
import com.pypath.app.ui.components.AppTopBar
import com.pypath.app.ui.components.ButtonKind
import com.pypath.app.ui.components.Eyebrow
import com.pypath.app.ui.components.Pill
import com.pypath.app.ui.theme.AppTheme

private enum class StepUi { DONE, CURRENT, LOCKED, SOON }

@Composable
fun SubLevelScreen(
    snapshot: CourseSnapshot,
    subLevelId: String,
    onBack: () -> Unit,
    onOpened: () -> Unit,
    onStartLesson: () -> Unit,
    onStartQuiz: () -> Unit,
) {
    val c = AppTheme.colors
    val s = snapshot.subLevel(subLevelId) ?: return
    LaunchedEffect(subLevelId) { if (s.isPlayable) onOpened() }

    val p = s.progress
    val learnDone = StepType.LEARN in p.completedSteps
    val quizDone = StepType.QUIZ in p.completedSteps
    val index = s.indexInLevel + 1

    Column(Modifier.fillMaxSize().background(c.background)) {
        AppTopBar(
            title = "${s.subLevel.code} ${s.subLevel.title}",
            subtitle = "Level ${s.level.number} · ${s.level.title}",
            onBack = onBack,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Eyebrow("Sub-level $index of ${s.level.subLevels.size}")
            Spacer(Modifier.height(6.dp))
            Text(s.subLevel.title, style = MaterialTheme.typography.headlineMedium, color = c.text)
            Spacer(Modifier.height(8.dp))
            Text(s.subLevel.summary, style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("${s.subLevel.estimatedMinutes} min", c.brand, c.brandSoft)
                s.subLevel.quiz?.let { Pill("${it.questions.size} questions", c.textMuted, c.surfaceAlt) }
                if (s.status == NodeStatus.COMPLETED) Pill("Completed", c.success, c.successSoft, icon = Icons.Filled.Check)
            }

            Spacer(Modifier.height(28.dp))
            Text("Steps", style = MaterialTheme.typography.titleLarge, color = c.text)
            Spacer(Modifier.height(12.dp))

            if (!s.isPlayable) {
                AppCard(Modifier.fillMaxWidth()) {
                    Text("This sub-level is locked", style = MaterialTheme.typography.titleSmall, color = c.text)
                    Text("Finish ${s.unlockedBy?.code ?: "the previous sub-level"} first.", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            } else {
                val steps = s.subLevel.steps.toMutableList()
                if (StepType.PRACTICE !in steps) steps += StepType.PRACTICE // preview of what's coming
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    steps.forEachIndexed { i, step ->
                        val ui = when {
                            !step.isSupported -> StepUi.SOON
                            step in p.completedSteps -> StepUi.DONE
                            step == StepType.LEARN -> StepUi.CURRENT
                            step == StepType.QUIZ && learnDone -> StepUi.CURRENT
                            else -> StepUi.LOCKED
                        }
                        val detail = when (step) {
                            StepType.LEARN -> "${s.subLevel.lesson?.pages?.size ?: 0} short pages with code examples"
                            StepType.QUIZ -> {
                                val q = p.quiz
                                when {
                                    q != null && quizDone -> "Best score ${q.bestScore}/${q.total}"
                                    q != null -> "Last try ${q.lastScore}/${q.total} · need ${s.subLevel.quiz?.passPercent ?: 60}% to pass"
                                    !learnDone -> "Unlocks after the lesson"
                                    else -> "Answer ${s.subLevel.quiz?.questions?.size ?: 0} questions about this lesson"
                                }
                            }
                            else -> "Hands-on coding is coming in a future update"
                        }
                        StepRow(i + 1, step.label, detail, ui)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (s.isPlayable) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    !learnDone -> AppButton("Start lesson", onStartLesson, Modifier.fillMaxWidth(), leading = Icons.Filled.PlayArrow)
                    !quizDone -> {
                        AppButton(if (p.quiz == null) "Start MCQs" else "Retry MCQs", onStartQuiz, Modifier.fillMaxWidth(), trailing = Icons.AutoMirrored.Filled.ArrowForward)
                        AppButton("Review lesson", onStartLesson, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary)
                    }
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppButton("Review lesson", onStartLesson, Modifier.weight(1f), kind = ButtonKind.Secondary)
                        AppButton("Retake MCQs", onStartQuiz, Modifier.weight(1f), kind = ButtonKind.Secondary, leading = Icons.Filled.Refresh)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(n: Int, title: String, detail: String, ui: StepUi) {
    val c = AppTheme.colors
    val (bg, fg, icon) = when (ui) {
        StepUi.DONE -> Triple(c.success, Color.White, Icons.Filled.Check as ImageVector?)
        StepUi.CURRENT -> Triple(c.brand, Color.White, null)
        StepUi.LOCKED -> Triple(c.surfaceAlt, c.locked, Icons.Filled.Lock)
        StepUi.SOON -> Triple(c.surfaceAlt, c.locked, Icons.Filled.Info)
    }
    AppCard(
        Modifier.fillMaxWidth(),
        borderColor = if (ui == StepUi.CURRENT) c.brand.copy(alpha = 0.5f) else c.border,
        background = if (ui == StepUi.SOON) c.surface.copy(alpha = 0.55f) else c.surface,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
                if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                else Text("$n", style = MaterialTheme.typography.titleSmall, color = fg, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (ui == StepUi.SOON || ui == StepUi.LOCKED) c.textMuted else c.text)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
            when (ui) {
                StepUi.DONE -> Pill("Done", c.success, c.successSoft)
                StepUi.SOON -> Pill("Soon", c.textFaint, c.surfaceAlt)
                else -> {}
            }
        }
    }
}
