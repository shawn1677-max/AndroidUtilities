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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.BigReadout
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.StopwatchStore
import com.utilitybox.app.util.formatStopwatch
import com.utilitybox.app.widget.StopwatchWidgetProvider
import kotlinx.coroutines.delay

@Composable
fun StopwatchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // The reading lives in StopwatchStore so this screen and the home screen
    // widget drive one stopwatch rather than two that quietly disagree.
    var state by remember { mutableStateOf(StopwatchStore.read(context)) }
    var elapsed by remember {
        mutableLongStateOf(state.elapsedAt(SystemClock.elapsedRealtime()))
    }
    val laps = remember { mutableStateListOf<Long>() }
    val running = state.running

    // elapsedRealtime is monotonic, so the reading stays correct across clock changes.
    LaunchedEffect(state) {
        elapsed = state.elapsedAt(SystemClock.elapsedRealtime())
        while (state.running) {
            elapsed = state.elapsedAt(SystemClock.elapsedRealtime())
            delay(16)
        }
    }

    // The widget can start, pause or reset while this screen is in the background.
    LifecycleResumeEffect(Unit) {
        val restored = StopwatchStore.read(context)
        state = restored
        elapsed = restored.elapsedAt(SystemClock.elapsedRealtime())
        // Laps belong to this screen; drop them if the timer was reset elsewhere.
        if (laps.isNotEmpty() && elapsed < (laps.firstOrNull() ?: 0L)) laps.clear()
        onPauseOrDispose { }
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
                    state = StopwatchStore.toggle(context)
                    StopwatchWidgetProvider.refresh(context)
                },
                modifier = Modifier.weight(1f),
            ) { Text(if (running) "Pause" else if (elapsed > 0) "Resume" else "Start") }

            OutlinedButton(
                onClick = {
                    if (running) {
                        laps.add(0, elapsed)
                    } else {
                        state = StopwatchStore.reset(context)
                        elapsed = 0
                        laps.clear()
                        StopwatchWidgetProvider.refresh(context)
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
