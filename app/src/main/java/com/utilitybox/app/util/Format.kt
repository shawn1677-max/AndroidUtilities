package com.utilitybox.app.util

import java.text.DecimalFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private val oneDecimal = DecimalFormat("0.#")

/** Human readable byte count using binary (1024) units, matching how Android reports storage. */
fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "—"
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB", "PB")
    var value = bytes.toDouble() / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return "${oneDecimal.format(value)} ${units[unitIndex]}"
}

/** "2 d 3 h 14 m" style duration, used for uptime. */
fun formatUptime(millis: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(millis)
    val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return buildString {
        if (days > 0) append("${days}d ")
        if (days > 0 || hours > 0) append("${hours}h ")
        if (days > 0 || hours > 0 || minutes > 0) append("${minutes}m ")
        append("${seconds}s")
    }
}

/** mm:ss.cc used by the stopwatch and timer. */
fun formatStopwatch(millis: Long): String {
    val safe = millis.coerceAtLeast(0)
    val hours = TimeUnit.MILLISECONDS.toHours(safe)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(safe) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(safe) % 60
    val hundredths = (safe % 1000) / 10
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, hundredths)
    } else {
        String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
    }
}

/** Rounds to [decimals] places without scientific notation or trailing zero noise. */
fun formatNumber(value: Double, decimals: Int = 2): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val pattern = if (decimals <= 0) "0" else "0." + "#".repeat(decimals)
    return DecimalFormat(pattern).format(value)
}
