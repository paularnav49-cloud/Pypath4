package com.pypath.app.ui.mcq

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pypath.app.data.model.Question
import com.pypath.app.ui.components.CodeBlock
import com.pypath.app.ui.components.Eyebrow
import com.pypath.app.ui.theme.AppTheme

enum class OptionState { IDLE, SELECTED, CORRECT, WRONG, DIMMED }

/** Question prompt + optional code + four options. Pure UI driven by [McqSession]. */
@Composable
fun McqQuestionCard(session: McqSession, modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    val q = session.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow("Question ${session.index + 1} of ${session.total}")
        Text(q.prompt, style = MaterialTheme.typography.headlineSmall, color = c.text)
        q.code?.let { CodeBlock(it) }
        Spacer(Modifier.height(4.dp))
        q.options.forEachIndexed { i, text ->
            val state = optionState(session, q, i)
            McqOption(letter = ('A' + i).toString(), text = text, state = state, onClick = { session.select(i) })
        }
        AnimatedVisibility(session.phase == McqPhase.FEEDBACK, enter = fadeIn(tween(250)) + expandVertically(tween(250))) {
            FeedbackPanel(correct = session.isCorrect, explanation = q.explanation, correctAnswer = q.options[q.correctIndex])
        }
    }
}

private fun optionState(s: McqSession, q: Question, i: Int): OptionState = when (s.phase) {
    McqPhase.ANSWERING -> if (s.selected == i) OptionState.SELECTED else OptionState.IDLE
    else -> when {
        i == q.correctIndex -> OptionState.CORRECT
        i == s.selected -> OptionState.WRONG
        else -> OptionState.DIMMED
    }
}

@Composable
fun McqOption(letter: String, text: String, state: OptionState, onClick: () -> Unit) {
    val c = AppTheme.colors
    val border by animateColorAsState(
        when (state) {
            OptionState.SELECTED -> c.brand; OptionState.CORRECT -> c.success; OptionState.WRONG -> c.error; else -> c.border
        }, label = "ob",
    )
    val bg by animateColorAsState(
        when (state) {
            OptionState.SELECTED -> c.brandSoft; OptionState.CORRECT -> c.successSoft; OptionState.WRONG -> c.errorSoft; else -> c.surface
        }, label = "obg",
    )
    // Small shake for wrong answers, pop for correct ones.
    val shake = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(state) {
        when (state) {
            OptionState.WRONG -> for (x in listOf(10f, -8f, 6f, -4f, 0f)) shake.animateTo(x, tween(55))
            OptionState.CORRECT -> { pop.animateTo(1.03f, tween(120)); pop.animateTo(1f, tween(160)) }
            else -> {}
        }
    }
    val shape = RoundedCornerShape(16.dp)
    val interactive = state == OptionState.IDLE || state == OptionState.SELECTED
    Row(
        Modifier.fillMaxWidth()
            .graphicsLayer { translationX = shake.value; scaleX = pop.value; scaleY = pop.value; alpha = if (state == OptionState.DIMMED) 0.55f else 1f }
            .clip(shape).background(bg).border(if (state == OptionState.IDLE || state == OptionState.DIMMED) 1.dp else 2.dp, border, shape)
            .clickable(enabled = interactive, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (badgeBg, badgeFg) = when (state) {
            OptionState.SELECTED -> c.brand to Color.White
            OptionState.CORRECT -> c.success to Color.White
            OptionState.WRONG -> c.error to Color.White
            else -> c.surfaceAlt to c.textMuted
        }
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(badgeBg), contentAlignment = Alignment.Center) {
            when (state) {
                OptionState.CORRECT -> Icon(Icons.Filled.Check, null, tint = badgeFg, modifier = Modifier.size(18.dp))
                OptionState.WRONG -> Icon(Icons.Filled.Close, null, tint = badgeFg, modifier = Modifier.size(18.dp))
                else -> Text(letter, style = MaterialTheme.typography.titleSmall, color = badgeFg, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FeedbackPanel(correct: Boolean, explanation: String, correctAnswer: String) {
    val c = AppTheme.colors
    val accent = if (correct) c.success else c.error
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(16.dp))
            .background(if (correct) c.successSoft else c.errorSoft).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Icon(if (correct) Icons.Filled.Check else Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(if (correct) "Correct!" else "Not quite", style = MaterialTheme.typography.titleMedium, color = accent)
        }
        if (!correct) {
            Spacer(Modifier.height(8.dp))
            Text("Correct answer: $correctAnswer", style = MaterialTheme.typography.titleSmall, color = c.text)
        }
        Spacer(Modifier.height(6.dp))
        Text(explanation, style = MaterialTheme.typography.bodyMedium, color = c.text.copy(alpha = 0.85f))
    }
}
