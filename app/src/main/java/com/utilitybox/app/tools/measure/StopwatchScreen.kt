package com.utilitybox.app.tools.measure

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.BigReadout
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.formatStopwatch
import kotlinx.coroutines.delay

@Composable
fun StopwatchScreen(onBack: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var accumulated by remember { mutableLongStateOf(0L) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<Long>() }

    // elapsedRealtime is monotonic, so the reading stays correct across clock changes.
    LaunchedEffect(running) {
        while (running) {
            elapsed = accumulated + (SystemClock.elapsedRealtime() - startedAt)
            delay(16)
        }
    }

    ToolScaffold(title = "Stopwatch", onBack = onBack) {
        SectionCard {
            BigReadout(value = formatStopwatch(elapsed))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    if (running) {
                        accumulated += SystemClock.elapsedRealtime() - startedAt
                        elapsed = accumulated
                        running = false
                    } else {
                        startedAt = SystemClock.elapsedRealtime()
                        running = true
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text(if (running) "Pause" else if (elapsed > 0) "Resume" else "Start") }

            OutlinedButton(
                onClick = {
                    if (running) {
                        laps.add(0, elapsed)
                    } else {
                        running = false
                        accumulated = 0
                        elapsed = 0
                        laps.clear()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = running || elapsed > 0,
            ) { Text(if (running) "Lap" else "Reset") }
        }

        if (laps.isNotEmpty()) {
            SectionCard(title = "Laps") {
                laps.forEachIndexed { index, lapTotal ->
                    val lapNumber = laps.size - index
                    val previous = laps.getOrNull(index + 1) ?: 0L
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Lap $lapNumber",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                formatStopwatch(lapTotal - previous),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                formatStopwatch(lapTotal),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (index < laps.lastIndex) HorizontalDivider()
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        HintText(
            "The left column is the split for that lap, the right column is the total " +
                "elapsed time when the lap was taken."
        )
    }
}
