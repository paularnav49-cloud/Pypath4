package com.pypath.app.ui.screens.practical

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pypath.app.practical.EngineState
import com.pypath.app.practical.PracticalTask
import com.pypath.app.practical.PythonRunner
import com.pypath.app.practical.RunOutcome
import com.pypath.app.practical.RunnerListener
import com.pypath.app.practical.loadPracticalCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ConsoleKind { STDOUT, STDERR, INPUT, SYSTEM }

data class ConsoleSegment(val kind: ConsoleKind, val text: String)

enum class RunPhase { IDLE, STARTING, RUNNING, WAITING_INPUT }

/** Result of the most recent run, shown in the status pill and error card. */
data class RunResult(
    val kind: Kind,
    val elapsedMs: Long,
    val exitCode: Int = 0,
    /** Last line of the traceback, e.g. "NameError: name 'x' is not defined". */
    val errorSummary: String? = null,
    /** Line in main.py where the error happened, parsed from the real traceback. */
    val errorLine: Int? = null,
) {
    enum class Kind { SUCCESS, ERROR, EXITED, TIMEOUT, STOPPED, CRASHED, INTERNAL }
}

/**
 * State for the Practical tab. Code lives only in memory for this app session: it is never
 * written to disk, uploaded or synced. Activity-scoped, so it survives tab switches and rotation.
 */
class PracticalViewModel(app: Application) : AndroidViewModel(app), RunnerListener {

    var tasks by mutableStateOf<List<PracticalTask>>(emptyList()); private set
    var loadError by mutableStateOf<String?>(null); private set
    var selectedTaskId by mutableStateOf<String?>(null); private set
    private val drafts = mutableStateMapOf<String, TextFieldValue>()

    val console = mutableStateListOf<ConsoleSegment>()
    var engine by mutableStateOf<EngineState>(EngineState.Starting); private set
    var phase by mutableStateOf(RunPhase.IDLE); private set
    var inputPrompt by mutableStateOf(""); private set
    var lastResult by mutableStateOf<RunResult?>(null); private set
    var elapsedMs by mutableStateOf(0L); private set
    var hasRunOnce by mutableStateOf(false); private set
    private var stderrBuffer = StringBuilder()
    private var consoleChars = 0

    private val runner = PythonRunner(app, this)

    init {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { loadPracticalCatalog(getApplication()) } }
                .onSuccess { tasks = it.tasks }
                .onFailure { loadError = it.message ?: "Could not load practical tasks" }
        }
    }

    /** Called when the Practical tab is shown: starts the interpreter in the background. */
    fun warmUp() = runner.start()

    val selectedTask: PracticalTask? get() = tasks.firstOrNull { it.id == selectedTaskId }

    fun select(taskId: String) {
        if (taskId == selectedTaskId) return
        if (phase == RunPhase.RUNNING || phase == RunPhase.WAITING_INPUT) runner.stop()
        selectedTaskId = taskId
        console.clear(); consoleChars = 0
        lastResult = null
        hasRunOnce = false
    }

    /** Picks a default task the first time the tab opens. */
    fun ensureSelection(defaultId: String?) {
        if (selectedTaskId == null && defaultId != null) selectedTaskId = defaultId
    }

    fun code(task: PracticalTask): TextFieldValue =
        drafts[task.id] ?: TextFieldValue(task.starterCode, TextRange(task.starterCode.length))

    fun updateCode(task: PracticalTask, value: TextFieldValue) { drafts[task.id] = value }

    fun resetCode(task: PracticalTask) {
        drafts.remove(task.id)
        if (lastResult?.errorLine != null) lastResult = lastResult?.copy(errorLine = null)
    }

    fun run(task: PracticalTask) {
        if (phase != RunPhase.IDLE) return
        val code = code(task).text
        console.clear(); consoleChars = 0
        stderrBuffer = StringBuilder()
        lastResult = null
        hasRunOnce = true
        elapsedMs = 0
        phase = if (engine is EngineState.Ready) RunPhase.RUNNING else RunPhase.STARTING
        append(ConsoleKind.SYSTEM, if (phase == RunPhase.STARTING) "Starting Python…\n" else "")
        runner.run(code)
        viewModelScope.launch {
            while (isActive && phase != RunPhase.IDLE) {
                elapsedMs = runner.elapsedMs()
                delay(100)
            }
        }
    }

    fun stop() = runner.stop()

    fun submitInput(text: String) {
        if (phase != RunPhase.WAITING_INPUT) return
        append(ConsoleKind.INPUT, text + "\n")
        phase = RunPhase.RUNNING
        inputPrompt = ""
        runner.sendInput(text)
    }

    fun clearConsole() {
        console.clear(); consoleChars = 0
    }

    // ───────────── RunnerListener (main thread) ─────────────

    override fun onEngineState(state: EngineState) { engine = state }

    override fun onStarted() {
        if (phase == RunPhase.STARTING) {
            phase = RunPhase.RUNNING
            // drop the "Starting Python…" line now that code is running
            if (console.firstOrNull()?.kind == ConsoleKind.SYSTEM) { consoleChars -= console.first().text.length; console.removeAt(0) }
        }
    }

    override fun onOutput(stderr: Boolean, text: String) {
        if (stderr) stderrBuffer.append(text).also { if (it.length > 20_000) it.delete(0, it.length - 20_000) }
        append(if (stderr) ConsoleKind.STDERR else ConsoleKind.STDOUT, text)
    }

    override fun onInputRequested(prompt: String) {
        inputPrompt = prompt
        phase = RunPhase.WAITING_INPUT
    }

    override fun onFinished(outcome: RunOutcome, elapsedMs: Long) {
        this.elapsedMs = elapsedMs
        phase = RunPhase.IDLE
        inputPrompt = ""
        val secs = formatSeconds(elapsedMs)
        val result = when (outcome) {
            is RunOutcome.Completed -> when {
                outcome.status == "ok" -> RunResult(RunResult.Kind.SUCCESS, elapsedMs)
                outcome.status == "error" -> {
                    val (summary, line) = parseTraceback(stderrBuffer.toString())
                    RunResult(RunResult.Kind.ERROR, elapsedMs, errorSummary = summary, errorLine = line)
                }
                outcome.status.startsWith("exit:") -> {
                    val code = outcome.status.removePrefix("exit:").toIntOrNull() ?: 1
                    RunResult(if (code == 0) RunResult.Kind.SUCCESS else RunResult.Kind.EXITED, elapsedMs, exitCode = code)
                }
                else -> RunResult(RunResult.Kind.INTERNAL, elapsedMs, errorSummary = outcome.status.removePrefix("internal:"))
            }
            RunOutcome.StoppedByUser -> RunResult(RunResult.Kind.STOPPED, elapsedMs)
            is RunOutcome.TimedOut -> RunResult(RunResult.Kind.TIMEOUT, elapsedMs)
            RunOutcome.Crashed -> RunResult(RunResult.Kind.CRASHED, elapsedMs)
        }
        lastResult = result
        val line = when (result.kind) {
            RunResult.Kind.SUCCESS -> if (result.exitCode == 0 && outcome is RunOutcome.Completed && outcome.status == "exit:0") "Program exited (code 0) · $secs" else "Finished in $secs"
            RunResult.Kind.ERROR -> "Finished with an error · $secs"
            RunResult.Kind.EXITED -> "Program exited with code ${result.exitCode} · $secs"
            RunResult.Kind.TIMEOUT -> "Execution timed out after ${runner.timeLimitMs / 1000} seconds and was stopped. Check for a loop that never ends."
            RunResult.Kind.STOPPED -> "Execution stopped by you after $secs"
            RunResult.Kind.CRASHED -> "The Python process ended unexpectedly (it may have run out of memory). A fresh one has been started."
            RunResult.Kind.INTERNAL -> "Python could not run the code: ${result.errorSummary}"
        }
        val needsBreak = console.lastOrNull()?.text?.endsWith("\n") == false
        append(ConsoleKind.SYSTEM, (if (needsBreak) "\n" else "") + "— $line\n")
    }

    private fun append(kind: ConsoleKind, text: String) {
        if (text.isEmpty()) return
        consoleChars += text.length
        val last = console.lastOrNull()
        if (last != null && last.kind == kind) console[console.size - 1] = last.copy(text = last.text + text)
        else console.add(ConsoleSegment(kind, text))
        // Keep the UI responsive: retain at most ~120k chars (Python caps output at 200k).
        while (consoleChars > MAX_CONSOLE_CHARS && console.size > 1) {
            consoleChars -= console.first().text.length
            console.removeAt(0)
        }
        if (consoleChars > MAX_CONSOLE_CHARS && console.size == 1) {
            val seg = console[0]
            val trimmed = seg.text.takeLast(MAX_CONSOLE_CHARS)
            console[0] = seg.copy(text = trimmed)
            consoleChars = trimmed.length
        }
    }

    override fun onCleared() {
        runner.release()
        super.onCleared()
    }

    companion object {
        private const val MAX_CONSOLE_CHARS = 120_000

        fun formatSeconds(ms: Long): String = if (ms < 10_000) "%.2fs".format(ms / 1000.0) else "%.1fs".format(ms / 1000.0)

        /** Extracts "ErrorType: message" and the last main.py line number from a real traceback. */
        fun parseTraceback(tb: String): Pair<String?, Int?> {
            val lines = tb.trimEnd().lines().filter { it.isNotBlank() }
            val summary = lines.lastOrNull()?.trim()
            val line = Regex("""File "main\.py", line (\d+)""").findAll(tb).lastOrNull()?.groupValues?.get(1)?.toIntOrNull()
            return summary to line
        }
    }
}
