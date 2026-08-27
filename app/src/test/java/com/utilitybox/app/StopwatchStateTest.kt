package com.utilitybox.app

import com.utilitybox.app.util.StopwatchState
import com.utilitybox.app.util.formatClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchStateTest {

    @Test
    fun `a fresh stopwatch reads zero and counts as cleared`() {
        val state = StopwatchState()
        assertEquals(0L, state.elapsedAt(5_000))
        assertTrue(state.isCleared)
    }

    @Test
    fun `a paused stopwatch ignores the passage of time`() {
        val state = StopwatchState(running = false, accumulatedMs = 4_200)
        assertEquals(4_200, state.elapsedAt(1_000))
        assertEquals(4_200, state.elapsedAt(9_999_999))
    }

    @Test
    fun `a running stopwatch counts from when it started`() {
        val state = StopwatchState().started(nowElapsedMs = 1_000)
        assertEquals(0L, state.elapsedAt(1_000))
        assertEquals(2_500, state.elapsedAt(3_500))
    }

    @Test
    fun `resuming adds to the time already banked`() {
        val state = StopwatchState(accumulatedMs = 10_000).started(nowElapsedMs = 500)
        assertEquals(10_000, state.elapsedAt(500))
        assertEquals(13_000, state.elapsedAt(3_500))
    }

    @Test
    fun `pausing banks the elapsed time and stops counting`() {
        val paused = StopwatchState().started(1_000).paused(4_000)
        assertFalse(paused.running)
        assertEquals(3_000, paused.accumulatedMs)
        assertEquals(3_000, paused.elapsedAt(99_000))
    }

    @Test
    fun `a start pause start pause cycle accumulates both runs`() {
        val state = StopwatchState()
            .started(0).paused(1_000)
            .started(5_000).paused(5_500)
        assertEquals(1_500, state.accumulatedMs)
    }

    @Test
    fun `starting an already running stopwatch does not restart it`() {
        val running = StopwatchState().started(1_000)
        assertEquals(running, running.started(9_000))
    }

    @Test
    fun `pausing an already paused stopwatch is a no-op`() {
        val paused = StopwatchState(accumulatedMs = 700)
        assertEquals(paused, paused.paused(9_000))
    }

    @Test
    fun `a reboot while running never counts time backwards`() {
        // elapsedRealtime restarts at zero on reboot, so "now" can be less than
        // the moment the stopwatch was started.
        val state = StopwatchState(running = true, accumulatedMs = 8_000, startedAtElapsedMs = 500_000)
        assertEquals(8_000, state.elapsedAt(nowElapsedMs = 120))
        assertTrue(state.elapsedAt(0) >= 0)
    }

    @Test
    fun `chronometer base is now minus the reading`() {
        val state = StopwatchState(accumulatedMs = 7_000).started(2_000)
        val now = 9_000L
        assertEquals(now - state.elapsedAt(now), state.chronometerBase(now))
    }

    @Test
    fun `clock format matches what a chronometer renders`() {
        assertEquals("00:00", formatClock(0))
        assertEquals("00:09", formatClock(9_400))
        assertEquals("01:00", formatClock(60_000))
        assertEquals("59:59", formatClock(3_599_000))
        assertEquals("1:00:00", formatClock(3_600_000))
        assertEquals("2:03:04", formatClock(7_384_000))
    }

    @Test
    fun `clock format truncates rather than rounds, like a chronometer`() {
        assertEquals("00:01", formatClock(1_999))
    }

    @Test
    fun `clock format never shows a negative time`() {
        assertEquals("00:00", formatClock(-5_000))
    }
}
