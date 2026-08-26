package com.utilitybox.app.tools.measure

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.BigReadout
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.KeepScreenOn
import com.utilitybox.app.util.MetronomeEngine
import kotlinx.coroutines.delay

@Composable
fun MetronomeScreen(onBack: () -> Unit) {
    val engine = remember { MetronomeEngine() }
    var bpm by remember { mutableIntStateOf(120) }
    var beatsPerBar by remember { mutableIntStateOf(4) }
    var running by remember { mutableStateOf(false) }
    var beat by remember { mutableIntStateOf(0) }
    val tapTimes = remember { mutableStateListOf<Long>() }

    DisposableEffect(engine) { onDispose { engine.stop() } }
    LaunchedEffect(bpm) { engine.setTempo(bpm) }
    LaunchedEffect(beatsPerBar) { engine.setBeatsPerBar(beatsPerBar) }
    LaunchedEffect(running) {
        if (running) {
            engine.setTempo(bpm)
            engine.setBeatsPerBar(beatsPerBar)
            engine.start()
            while (running) {
                beat = engine.beat
                delay(16)
            }
        } else {
            engine.stop()
            beat = 0
        }
    }

    KeepScreenOn(active = running)

    val currentBeatInBar = if (beat == 0) 0 else ((beat - 1) % beatsPerBar) + 1
    val flash by animateFloatAsState(
        targetValue = if (running) 1f else 0.35f,
        label = "beat-flash",
    )

    ToolScaffold(title = "Metronome", onBack = onBack) {
        SectionCard {
            BigReadout(value = bpm.toString(), unit = "BPM")
            Spacer(Modifier.height(4.dp))
            Text(
                text = tempoName(bpm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(beatsPerBar) { index ->
                val active = running && currentBeatInBar == index + 1
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (index == 0) 26.dp else 20.dp)
                        .background(
                            color = when {
                                active && index == 0 -> MaterialTheme.colorScheme.primary
                                active -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }.copy(alpha = if (active) flash else 0.6f),
                            shape = CircleShape,
                        ),
                )
            }
        }

        Slider(
            value = bpm.toFloat(),
            onValueChange = { bpm = it.toInt() },
            valueRange = 30f..260f,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(-5, -1, 1, 5).forEach { delta ->
                OutlinedButton(
                    onClick = { bpm = (bpm + delta).coerceIn(30, 260) },
                    modifier = Modifier.weight(1f),
                ) { Text(if (delta > 0) "+$delta" else "$delta") }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { running = !running },
                modifier = Modifier.weight(1f),
            ) { Text(if (running) "Stop" else "Start") }

            OutlinedButton(
                onClick = {
                    val now = SystemClock.elapsedRealtime()
                    // Drop taps older than three seconds so a new tempo is picked up quickly.
                    tapTimes.removeAll { now - it > 3000 }
                    tapTimes.add(now)
                    if (tapTimes.size >= 2) {
                        val intervals = tapTimes.zipWithNext { a, b -> b - a }
                        val average = intervals.average()
                        if (average > 0) bpm = (60_000.0 / average).toInt().coerceIn(30, 260)
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("Tap tempo") }
        }

        SectionCard(title = "Beats per bar") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 4, 6).forEach { value ->
                    FilterChip(
                        selected = beatsPerBar == value,
                        onClick = { beatsPerBar = value },
                        label = { Text("$value") },
                    )
                }
            }
        }

        HintText(
            "The first beat of each bar is accented. Tap tempo four or more times in rhythm " +
                "to set the speed by ear."
        )
    }
}

private fun tempoName(bpm: Int): String = when {
    bpm < 40 -> "Grave"
    bpm < 60 -> "Largo"
    bpm < 66 -> "Larghetto"
    bpm < 76 -> "Adagio"
    bpm < 108 -> "Andante"
    bpm < 120 -> "Moderato"
    bpm < 156 -> "Allegro"
    bpm < 176 -> "Vivace"
    bpm < 200 -> "Presto"
    else -> "Prestissimo"
}
