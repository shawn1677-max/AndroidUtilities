package com.utilitybox.app.tools.hardware

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold

@Composable
fun VibrationTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vibrator = remember { context.vibrator() }
    val hasVibrator = vibrator?.hasVibrator() == true
    val amplitudeControl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        vibrator?.hasAmplitudeControl() == true

    var duration by remember { mutableFloatStateOf(300f) }
    var amplitude by remember { mutableFloatStateOf(255f) }

    DisposableEffect(Unit) { onDispose { runCatching { vibrator?.cancel() } } }

    ToolScaffold(title = "Vibration Test", onBack = onBack) {
        if (!hasVibrator) {
            SectionCard {
                Text(
                    "This device does not report a vibration motor.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@ToolScaffold
        }

        SectionCard(title = "Single pulse") {
            Text(
                "Duration: ${duration.toInt()} ms",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = duration,
                onValueChange = { duration = it },
                valueRange = 20f..2000f,
            )

            if (amplitudeControl) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Strength: ${(amplitude / 255f * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = amplitude,
                    onValueChange = { amplitude = it },
                    valueRange = 1f..255f,
                )
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vibrator?.vibrateOnce(duration.toLong(), amplitude.toInt(), amplitudeControl)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Vibrate") }
        }

        SectionCard(title = "Patterns") {
            val patterns = listOf(
                "Short click" to longArrayOf(0, 40),
                "Double tap" to longArrayOf(0, 60, 80, 60),
                "Heartbeat" to longArrayOf(0, 120, 100, 220, 500),
                "Escalating" to longArrayOf(0, 60, 60, 120, 60, 240, 60, 480),
                "SOS" to longArrayOf(
                    0, 150, 100, 150, 100, 150, 300,
                    450, 100, 450, 100, 450, 300,
                    150, 100, 150, 100, 150,
                ),
            )
            patterns.forEach { (name, pattern) ->
                OutlinedButton(
                    onClick = { vibrator?.vibratePattern(pattern) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                ) { Text(name) }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vibrator?.cancel() }) { Text("Stop") }
            }
        }

        SectionCard(title = "Capabilities") {
            InfoRow("Vibrator present", "Yes")
            InfoRow("Amplitude control", if (amplitudeControl) "Yes" else "No")
            InfoRow(
                "Predefined effects",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "Supported" else "Not on this API level",
            )
        }

        HintText(
            "A healthy haptic motor produces an even buzz with no rattle and stops cleanly. " +
                "If nothing happens, check that vibration is enabled in system sound settings."
        )
    }
}

@Suppress("DEPRECATION")
private fun Context.vibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

@Suppress("DEPRECATION")
private fun Vibrator.vibrateOnce(millis: Long, amplitude: Int, amplitudeControl: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = if (amplitudeControl) {
            VibrationEffect.createOneShot(millis, amplitude.coerceIn(1, 255))
        } else {
            VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrate(effect)
    } else {
        vibrate(millis)
    }
}

@Suppress("DEPRECATION")
private fun Vibrator.vibratePattern(pattern: LongArray) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrate(VibrationEffect.createWaveform(pattern, -1))
    } else {
        vibrate(pattern, -1)
    }
}
