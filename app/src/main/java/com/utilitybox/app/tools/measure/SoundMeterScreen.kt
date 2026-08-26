package com.utilitybox.app.tools.measure

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.BigReadout
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.PermissionGate
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.math.log10
import kotlin.math.sqrt

private const val SAMPLE_RATE = 44_100
private const val HISTORY_SIZE = 120

@Composable
fun SoundMeterScreen(onBack: () -> Unit) {
    ToolScaffold(title = "Sound Meter", onBack = onBack) {
        PermissionGate(
            permission = Manifest.permission.RECORD_AUDIO,
            rationale = "The sound meter measures how loud the room is, which needs access " +
                "to the microphone. Audio is analysed instantly and never recorded, saved " +
                "or sent anywhere — this app has no internet permission at all.",
        ) {
            SoundMeterContent()
        }
    }
}

@Composable
private fun SoundMeterContent() {
    var current by remember { mutableFloatStateOf(0f) }
    var minimum by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var maximum by remember { mutableFloatStateOf(0f) }
    var calibration by remember { mutableFloatStateOf(94f) }
    var error by remember { mutableStateOf<String?>(null) }
    val history = remember { mutableStateListOf<Float>() }

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    DisposableEffect(Unit) {
        val job = scope.launch(Dispatchers.Default) {
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBuffer <= 0) {
                withContext(Dispatchers.Main) { error = "Microphone is unavailable" }
                return@launch
            }
            val bufferSize = minBuffer * 2
            val recorder = runCatching {
                @Suppress("MissingPermission")
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                )
            }.getOrNull()

            if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder?.release()
                withContext(Dispatchers.Main) { error = "Could not open the microphone" }
                return@launch
            }

            val buffer = ShortArray(bufferSize / 2)
            runCatching { recorder.startRecording() }
            try {
                while (coroutineContext.isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    var sumSquares = 0.0
                    for (i in 0 until read) {
                        val normalised = buffer[i] / 32768.0
                        sumSquares += normalised * normalised
                    }
                    val rms = sqrt(sumSquares / read)
                    if (rms <= 0.0) continue
                    val dbfs = 20.0 * log10(rms)
                    withContext(Dispatchers.Main) {
                        val level = (dbfs + calibration).toFloat().coerceIn(0f, 140f)
                        current = level
                        if (level < minimum) minimum = level
                        if (level > maximum) maximum = level
                        history.add(level)
                        if (history.size > HISTORY_SIZE) history.removeAt(0)
                    }
                }
            } finally {
                runCatching { recorder.stop() }
                recorder.release()
            }
        }
        onDispose { job.cancel() }
    }

    error?.let { message ->
        SectionCard { Text(message, style = MaterialTheme.typography.bodyMedium) }
        return
    }

    SectionCard {
        BigReadout(value = String.format(Locale.US, "%.1f", current), unit = "dB")
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (current / 120f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = loudnessDescription(current),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    SectionCard(title = "Level history") {
        LevelGraph(history)
    }

    SectionCard(title = "Session") {
        InfoRow(
            "Minimum",
            if (minimum == Float.MAX_VALUE) "—" else String.format(Locale.US, "%.1f dB", minimum),
        )
        InfoRow("Maximum", String.format(Locale.US, "%.1f dB", maximum))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                minimum = Float.MAX_VALUE
                maximum = 0f
                history.clear()
            }) { Text("Reset") }
        }
    }

    SectionCard(title = "Calibration") {
        Text(
            String.format(Locale.US, "Offset %.0f dB", calibration),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = calibration,
            onValueChange = { calibration = it },
            valueRange = 70f..120f,
        )
        HintText(
            "Phone microphones are not calibrated instruments and most compress loud sounds. " +
                "If you have a reference meter, match it with this offset. Treat readings as " +
                "an indication, not a legally defensible measurement."
        )
    }
}

@Composable
private fun LevelGraph(history: List<Float>) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        // Horizontal guides every 20 dB up to 120 dB.
        for (level in 0..120 step 20) {
            val y = size.height * (1f - level / 120f)
            drawLine(grid.copy(alpha = 0.3f), Offset(0f, y), Offset(size.width, y), 1f)
        }
        if (history.size < 2) return@Canvas
        val stepX = size.width / (HISTORY_SIZE - 1)
        var previous: Offset? = null
        history.forEachIndexed { index, value ->
            val point = Offset(index * stepX, size.height * (1f - (value / 120f).coerceIn(0f, 1f)))
            previous?.let { drawLine(line, it, point, 3f) }
            previous = point
        }
    }
}

private fun loudnessDescription(db: Float): String = when {
    db < 30 -> "Very quiet — like a whisper in a still room"
    db < 45 -> "Quiet — a library or a calm bedroom"
    db < 60 -> "Moderate — normal conversation"
    db < 75 -> "Noisy — a busy office or street"
    db < 85 -> "Loud — heavy traffic, prolonged exposure is tiring"
    db < 100 -> "Very loud — hearing protection is advisable"
    else -> "Extremely loud — protect your hearing"
}
