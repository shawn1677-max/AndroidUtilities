package com.utilitybox.app

import com.utilitybox.app.util.formatBytes
import com.utilitybox.app.util.formatNumber
import com.utilitybox.app.util.formatStopwatch
import com.utilitybox.app.util.formatUptime
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `byte counts use binary units`() {
        assertEquals("512 B", formatBytes(512))
        assertEquals("1 KB", formatBytes(1024))
        assertEquals("1 MB", formatBytes(1024L * 1024))
        assertEquals("1.5 GB", formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `negative byte counts are shown as unknown`() {
        assertEquals("—", formatBytes(-1))
    }

    @Test
    fun `stopwatch drops the hour field until it is needed`() {
        assertEquals("00:00.00", formatStopwatch(0))
        assertEquals("01:05.25", formatStopwatch(65_250))
        assertEquals("1:00:00.00", formatStopwatch(3_600_000))
    }

    @Test
    fun `stopwatch never shows a negative time`() {
        assertEquals("00:00.00", formatStopwatch(-5_000))
    }

    @Test
    fun `uptime grows its fields as the duration grows`() {
        assertEquals("42s", formatUptime(42_000))
        assertEquals("3m 0s", formatUptime(180_000))
        assertEquals("1d 0h 0m 0s", formatUptime(86_400_000))
    }

    @Test
    fun `numbers drop insignificant trailing zeros`() {
        assertEquals("3.14", formatNumber(3.14159, 2))
        assertEquals("7", formatNumber(7.0, 2))
    }

    @Test
    fun `non finite numbers are shown as unknown`() {
        assertEquals("—", formatNumber(Double.NaN))
        assertEquals("—", formatNumber(Double.POSITIVE_INFINITY))
    }
}
