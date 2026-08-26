package com.utilitybox.app.tools.measure

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.LocalSnackbar
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.KeepScreenOn
import kotlinx.coroutines.delay
import java.util.Locale

private val PRESETS_SECONDS = listOf(30L, 60L, 180L, 300L, 600L, 1800L)

@Composable
fun TimerScreen(onBack: () -> Unit) {
    val snackbar = LocalSnackbar.current
    var durationMs by remember { mutableLongStateOf(300_000L) }
    var remainingMs by remember { mutableLongStateOf(300_000L) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 90) }.getOrNull()
    }
    DisposableEffect(Unit) { onDispose { toneGenerator?.release() } }

    KeepScreenOn(active = running)

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val endAt = SystemClock.elapsedRealtime() + remainingMs
        while (running) {
            val left = endAt - SystemClock.elapsedRealtime()
            if (left <= 0) {
                remainingMs = 0
                running = false
                finished = true
                repeat(3) {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400)
                    delay(600)
                }
                snackbar("Time is up")
                break
            }
            remainingMs = left
            delay(50)
        }
    }

    val progress = if (durationMs > 0) {
        (remainingMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }

    ToolScaffold(title = "Countdown Timer", onBack = onBack) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(220.dp),
                strokeWidth = 12.dp,
            )
            Text(
                text = formatCountdown(remainingMs),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Monospace,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    if (remainingMs <= 0) remainingMs = durationMs
                    finished = false
                    running = !running
                },
                modifier = Modifier.weight(1f),
                enabled = durationMs > 0,
            ) { Text(if (running) "Pause" else "Start") }

            OutlinedButton(
                onClick = {
                    running = false
                    finished = false
                    remainingMs = durationMs
                },
                modifier = Modifier.weight(1f),
            ) { Text("Reset") }
        }

        SectionCard(title = "Set duration") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-60L, -10L, 10L, 60L).forEach { deltaSeconds ->
                    OutlinedButton(
                        onClick = {
                            val next = (durationMs + deltaSeconds * 1000)
                                .coerceIn(1_000L, 24 * 60 * 60 * 1000L)
                            durationMs = next
                            if (!running) remainingMs = next
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                    ) {
                        Text(
                            text = if (deltaSeconds > 0) "+${labelFor(deltaSeconds)}" else "-${labelFor(-deltaSeconds)}",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESETS_SECONDS.take(3).forEach { seconds ->
                    FilterChip(
                        selected = durationMs == seconds * 1000,
                        onClick = {
                            running = false
                            durationMs = seconds * 1000
                            remainingMs = seconds * 1000
                        },
                        label = { Text(labelFor(seconds)) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESETS_SECONDS.drop(3).forEach { seconds ->
                    FilterChip(
                        selected = durationMs == seconds * 1000,
                        onClick = {
                            running = false
                            durationMs = seconds * 1000
                            remainingMs = seconds * 1000
                        },
                        label = { Text(labelFor(seconds)) },
                    )
                }
            }
        }

        HintText(
            if (finished) {
                "Finished. Press Start to run the same countdown again."
            } else {
                "The screen stays awake while the timer runs. The alert plays on the alarm " +
                    "volume stream, so raise that volume if you cannot hear it."
            }
        )
    }
}

private fun labelFor(seconds: Long): String = when {
    seconds % 3600 == 0L -> "${seconds / 3600} h"
    seconds % 60 == 0L -> "${seconds / 60} min"
    else -> "$seconds s"
}

private fun formatCountdown(millis: Long): String {
    val total = (millis + 999) / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
