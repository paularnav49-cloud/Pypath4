package com.pypath.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pypath.app.domain.CourseSnapshot
import com.pypath.app.domain.LevelState
import com.pypath.app.domain.NodeStatus
import com.pypath.app.ui.components.AppCard
import com.pypath.app.ui.components.Pill
import com.pypath.app.ui.components.ProgressBar
import com.pypath.app.ui.components.ProgressRing
import com.pypath.app.ui.components.StatusNode
import com.pypath.app.ui.screens.onboarding.Logo
import com.pypath.app.ui.theme.AppTheme
import java.util.Calendar

@Composable
fun DashboardScreen(
    snapshot: CourseSnapshot,
    onContinue: (String) -> Unit,
    onOpenLevel: (String) -> Unit,
    onResetProgress: () -> Unit,
) {
    val c = AppTheme.colors
    var menu by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(c.background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Logo()
            Spacer(Modifier.width(10.dp))
            Text("PyPath", style = MaterialTheme.typography.titleMedium, color = c.text)
            Spacer(Modifier.weight(1f))
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Menu", tint = c.textMuted) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Reset progress") },
                        leadingIcon = { Icon(Icons.Filled.Refresh, null) },
                        onClick = { menu = false; confirmReset = true },
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp,
                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(greeting(), style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
                Text(
                    if (snapshot.completedTotal == 0) "Let's start your Python journey" else "Ready for your next step?",
                    style = MaterialTheme.typography.headlineSmall, color = c.text,
                )
                Spacer(Modifier.height(8.dp))
            }
            item { HeroCard(snapshot, onContinue) }
            item { StatsRow(snapshot) }
            item {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Your path", style = MaterialTheme.typography.titleLarge, color = c.text, modifier = Modifier.weight(1f))
                    Text("${snapshot.levels.size} levels", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
                Spacer(Modifier.height(2.dp))
            }
            items(snapshot.levels, key = { it.level.id }) { lvl ->
                LevelCard(lvl, previous = snapshot.levels.getOrNull(snapshot.levels.indexOf(lvl) - 1), onClick = { onOpenLevel(lvl.level.id) })
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset progress?") },
            text = { Text("All completed sub-levels and MCQ scores will be cleared. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; onResetProgress() }) { Text("Reset", color = c.error) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
            containerColor = c.surface,
        )
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

// Hero keeps the same deep brand gradient in light and dark themes for contrast.
private val HeroStart = Color(0xFF3D5AFE)
private val HeroEnd = Color(0xFF2335C8)

@Composable
private fun HeroCard(snapshot: CourseSnapshot, onContinue: (String) -> Unit) {
    val c = AppTheme.colors
    val target = snapshot.continueTarget
    val current = snapshot.current
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(HeroStart, HeroEnd)))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (current != null) {
                    Text(
                        "LEVEL ${current.level.number} · ${current.level.title.uppercase()}",
                        style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (target != null) "${target.subLevel.code}  ${target.subLevel.title}" else "All caught up",
                        style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            target == null -> "You've finished every available sub-level. New levels are on the way."
                            target.status == NodeStatus.IN_PROGRESS -> "Pick up where you left off"
                            else -> "${target.subLevel.estimatedMinutes} min · Learn + MCQs"
                        },
                        style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            ProgressRing(
                fraction = snapshot.overallFraction, size = 68.dp, stroke = 7.dp,
                color = c.accent, track = Color.White.copy(alpha = 0.18f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(snapshot.overallFraction * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        if (target != null) {
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .clickable { onContinue(target.subLevel.id) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (snapshot.completedTotal == 0 && target.status == NodeStatus.AVAILABLE) "Start Learning" else "Continue Learning",
                    style = MaterialTheme.typography.labelLarge, color = HeroEnd,
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = HeroEnd, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun StatsRow(snapshot: CourseSnapshot) {
    val c = AppTheme.colors
    val scores = snapshot.orderedSubLevels.mapNotNull { it.progress.quiz?.takeIf { q -> q.total > 0 } }
    val avg = if (scores.isEmpty()) null else scores.sumOf { it.bestScore * 100 / it.total } / scores.size
    val currentLevel = snapshot.current?.level?.number ?: 1
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Sub-levels", "${snapshot.completedTotal}/${snapshot.playableTotal}", Modifier.weight(1f))
        StatCard("Current level", "$currentLevel of ${snapshot.levels.size}", Modifier.weight(1f))
        StatCard("MCQ average", avg?.let { "$it%" } ?: "—", Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    val c = AppTheme.colors
    AppCard(modifier, padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = c.text, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LevelCard(lvl: LevelState, previous: LevelState?, onClick: () -> Unit) {
    val c = AppTheme.colors
    val locked = lvl.status == NodeStatus.LOCKED || lvl.status == NodeStatus.COMING_SOON
    AppCard(
        Modifier.fillMaxWidth(),
        onClick = onClick,
        background = if (locked) c.surface.copy(alpha = 0.6f) else c.surface,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusNode(lvl.status, "${lvl.level.number}", size = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LEVEL ${lvl.level.number}", style = MaterialTheme.typography.labelSmall, color = if (locked) c.textFaint else c.brand)
                    Spacer(Modifier.width(8.dp))
                    when (lvl.status) {
                        NodeStatus.COMPLETED -> Pill("Completed", c.success, c.successSoft)
                        NodeStatus.COMING_SOON -> Pill("Coming soon", c.textFaint, c.surfaceAlt)
                        NodeStatus.IN_PROGRESS -> Pill("In progress", c.brand, c.brandSoft)
                        else -> {}
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(lvl.level.title, style = MaterialTheme.typography.titleMedium, color = if (locked) c.textMuted else c.text)
                Text(
                    when (lvl.status) {
                        NodeStatus.LOCKED -> "Complete Level ${previous?.level?.number ?: ""} to unlock"
                        else -> lvl.level.description
                    },
                    style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.textFaint)
        }
        if (!locked) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(lvl.fraction, Modifier.weight(1f), height = 6.dp, color = if (lvl.status == NodeStatus.COMPLETED) c.success else c.brand)
                Spacer(Modifier.width(10.dp))
                Text("${lvl.completedCount}/${lvl.total}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
        }
    }
}
