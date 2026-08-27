package com.utilitybox.app.util

import android.content.Context
import android.os.SystemClock
import androidx.core.content.edit
import java.util.Locale

private const val PREFS = "utilitybox_stopwatch"
private const val KEY_RUNNING = "running"
private const val KEY_ACCUMULATED = "accumulated_ms"
private const val KEY_STARTED_AT = "started_at_elapsed_ms"

/**
 * The stopwatch reading, expressed against [SystemClock.elapsedRealtime] so it
 * is immune to wall-clock changes.
 *
 * [startedAtElapsedMs] is only meaningful while [running]; when paused the whole
 * reading lives in [accumulatedMs].
 */
data class StopwatchState(
    val running: Boolean = false,
    val accumulatedMs: Long = 0L,
    val startedAtElapsedMs: Long = 0L,
) {
    /**
     * elapsedRealtime restarts at zero on reboot, which would make a stopwatch
     * left running before the reboot compute a negative run. Treat that as no
     * additional elapsed time rather than counting backwards.
     */
    fun elapsedAt(nowElapsedMs: Long): Long {
        if (!running) return accumulatedMs
        val sinceStart = nowElapsedMs - startedAtElapsedMs
        return if (sinceStart < 0) accumulatedMs else accumulatedMs + sinceStart
    }

    fun started(nowElapsedMs: Long): StopwatchState =
        if (running) this else copy(running = true, startedAtElapsedMs = nowElapsedMs)

    fun paused(nowElapsedMs: Long): StopwatchState =
        if (!running) this else StopwatchState(
            running = false,
            accumulatedMs = elapsedAt(nowElapsedMs),
            startedAtElapsedMs = 0L,
        )

    /** The base a [android.widget.Chronometer] needs to display this reading. */
    fun chronometerBase(nowElapsedMs: Long): Long = nowElapsedMs - elapsedAt(nowElapsedMs)

    val isCleared: Boolean get() = !running && accumulatedMs == 0L
}

/**
 * One stopwatch shared by the in-app tool and the home screen widget, so
 * starting it in one place and pausing it in the other behaves as a single
 * timer rather than two that quietly disagree.
 */
object StopwatchStore {

    fun read(context: Context): StopwatchState {
        val prefs = prefs(context)
        return StopwatchState(
            running = prefs.getBoolean(KEY_RUNNING, false),
            accumulatedMs = prefs.getLong(KEY_ACCUMULATED, 0L),
            startedAtElapsedMs = prefs.getLong(KEY_STARTED_AT, 0L),
        )
    }

    fun toggle(context: Context): StopwatchState {
        val now = SystemClock.elapsedRealtime()
        val current = read(context)
        return write(context, if (current.running) current.paused(now) else current.started(now))
    }

    fun reset(context: Context): StopwatchState = write(context, StopwatchState())

    fun write(context: Context, state: StopwatchState): StopwatchState {
        prefs(context).edit {
            putBoolean(KEY_RUNNING, state.running)
            putLong(KEY_ACCUMULATED, state.accumulatedMs)
            putLong(KEY_STARTED_AT, state.startedAtElapsedMs)
        }
        return state
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/**
 * Whole-second clock matching what a Chronometer renders, so the paused and
 * running widget states read identically. The in-app screen keeps hundredths;
 * a home screen widget cannot be redrawn fast enough for them to mean anything.
 */
fun formatClock(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
