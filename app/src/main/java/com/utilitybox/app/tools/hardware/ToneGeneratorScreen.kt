package com.utilitybox.app.tools.hardware

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.BigReadout
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.ToneChannel
import com.utilitybox.app.util.TonePlayer
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

private const val MIN_HZ = 20.0
private const val MAX_HZ = 20_000.0

@Composable
fun ToneGeneratorScreen(onBack: () -> Unit) {
    val player = remember { TonePlayer() }
    var playing by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(sliderFor(440.0)) }
    var volume by remember { mutableFloatStateOf(0.4f) }
    var channel by remember { mutableStateOf(ToneChannel.BOTH) }
    var sweeping by remember { mutableStateOf(false) }

    val frequency = frequencyFor(sliderPosition)

    DisposableEffect(player) { onDispose { player.stop() } }
    LaunchedEffect(frequency) { player.setFrequency(frequency) }
    LaunchedEffect(volume) { player.setVolume(volume) }
    LaunchedEffect(channel) { player.setChannel(channel) }
    LaunchedEffect(playing) {
        if (playing) {
            player.setFrequency(frequency)
            player.setVolume(volume)
            player.setChannel(channel)
            player.start()
        } else {
            sweeping = false
            player.stop()
        }
    }

    // A slow logarithmic sweep is the quickest way to hear a buzzing speaker.
    LaunchedEffect(sweeping) {
        if (!sweeping) return@LaunchedEffect
        var position = 0f
        while (sweeping) {
            position += 0.004f
            if (position > 1f) position = 0f
            sliderPosition = position
            delay(30)
        }
    }

    ToolScaffold(title = "Tone Generator", onBack = onBack) {
        SectionCard {
            BigReadout(value = String.format(Locale.US, "%,.0f", frequency), unit = "Hz")
            Spacer(Modifier.height(4.dp))
            Text(
                text = frequencyDescription(frequency),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Slider(
            value = sliderPosition,
            onValueChange = {
                sweeping = false
                sliderPosition = it
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { playing = !playing },
                modifier = Modifier.weight(1f),
            ) { Text(if (playing) "Stop" else "Play") }

            OutlinedButton(
                onClick = { sweeping = !sweeping },
                modifier = Modifier.weight(1f),
                enabled = playing,
            ) { Text(if (sweeping) "Stop sweep" else "Sweep") }
        }

        SectionCard(title = "Output channel") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ToneChannel.BOTH to "Both",
                    ToneChannel.LEFT to "Left",
                    ToneChannel.RIGHT to "Right",
                ).forEach { (option, label) ->
                    FilterChip(
                        selected = channel == option,
                        onClick = { channel = option },
                        label = { Text(label) },
                    )
                }
            }
        }

        SectionCard(title = "Volume") {
            Text(
                "${(volume * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(value = volume, onValueChange = { volume = it })
        }

        SectionCard(title = "Quick frequencies") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60.0, 440.0, 1000.0, 8000.0).forEach { hz ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            sweeping = false
                            sliderPosition = sliderFor(hz)
                        },
                        label = { Text(if (hz >= 1000) "${(hz / 1000).toInt()}k" else "${hz.toInt()}") },
                    )
                }
            }
        }

        HintText(
            "Start at a low volume. Use one channel at a time to check left/right balance, " +
                "and sweep slowly to find rattles or a blown driver. High frequencies above " +
                "15 kHz are inaudible to many adults — that is normal."
        )
    }
}

/** The slider is logarithmic so low frequencies get as much travel as high ones. */
private fun frequencyFor(position: Float): Double =
    MIN_HZ * (MAX_HZ / MIN_HZ).pow(position.coerceIn(0f, 1f).toDouble())

private fun sliderFor(hz: Double): Float =
    (ln(hz / MIN_HZ) / ln(MAX_HZ / MIN_HZ)).toFloat().coerceIn(0f, 1f)

private fun frequencyDescription(hz: Double): String = when {
    hz < 60 -> "Sub-bass — felt more than heard"
    hz < 250 -> "Bass"
    hz < 500 -> "Low midrange"
    hz < 2000 -> "Midrange — where speech sits"
    hz < 6000 -> "Presence — clarity and detail"
    hz < 12000 -> "Brilliance — cymbals and air"
    else -> "Very high — many adults cannot hear this"
}
