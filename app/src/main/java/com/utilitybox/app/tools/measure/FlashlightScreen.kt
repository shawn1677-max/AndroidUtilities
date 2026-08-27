package com.utilitybox.app.tools.measure

import android.content.Context
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.LocalSnackbar
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.torchCameraId
import com.utilitybox.app.widget.FlashlightWidgetProvider
import kotlinx.coroutines.delay
import java.util.Locale

private enum class TorchMode(val label: String) {
    STEADY("Steady"),
    STROBE("Strobe"),
    SOS("SOS"),
}

/** Dot, dash and gap lengths in units of one dot, per ITU Morse timing. */
private val SOS_PATTERN_MS = listOf(
    200L, 200L, 200L, 200L, 200L, 600L, // S, then letter gap
    600L, 200L, 600L, 200L, 600L, 600L, // O
    200L, 200L, 200L, 200L, 200L, 1400L, // S, then word gap
)

@Composable
fun FlashlightScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }
    val torchId = remember { cameraManager?.torchCameraId() }

    var enabled by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(TorchMode.STEADY) }
    var strobeHz by remember { mutableFloatStateOf(5f) }

    val currentTorchId = rememberUpdatedState(torchId)

    fun setTorch(on: Boolean) {
        val id = currentTorchId.value ?: return
        runCatching { cameraManager?.setTorchMode(id, on) }
            .onSuccess {
                // Steady mode is the only state worth mirroring; strobe and SOS
                // flip too fast for a widget redraw to mean anything.
                if (mode == TorchMode.STEADY) {
                    FlashlightWidgetProvider.onTorchStateChanged(context, on)
                }
            }
            .onFailure { snackbar("The torch is in use by another app") }
    }

    // Drives strobe and SOS; steady mode simply holds the torch on.
    LaunchedEffect(enabled, mode, strobeHz) {
        if (!enabled || torchId == null) {
            setTorch(false)
            return@LaunchedEffect
        }
        when (mode) {
            TorchMode.STEADY -> setTorch(true)

            TorchMode.STROBE -> {
                val halfPeriod = (500.0 / strobeHz).toLong().coerceAtLeast(20L)
                while (true) {
                    setTorch(true)
                    delay(halfPeriod)
                    setTorch(false)
                    delay(halfPeriod)
                }
            }

            TorchMode.SOS -> {
                while (true) {
                    SOS_PATTERN_MS.forEachIndexed { index, duration ->
                        setTorch(index % 2 == 0)
                        delay(duration)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { torchId?.let { cameraManager?.setTorchMode(it, false) } }
            FlashlightWidgetProvider.onTorchStateChanged(context, false)
        }
    }

    ToolScaffold(title = "Flashlight", onBack = onBack) {
        if (torchId == null) {
            SectionCard {
                Text(
                    "No camera flash was found on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@ToolScaffold
        }

        SectionCard {
            Button(
                onClick = { enabled = !enabled },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
            ) {
                Icon(
                    imageVector = if (enabled) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = if (enabled) "Turn off" else "Turn on",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        SectionCard(title = "Mode") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TorchMode.entries.forEach { option ->
                    FilterChip(
                        selected = mode == option,
                        onClick = { mode = option },
                        label = { Text(option.label) },
                    )
                }
            }

            if (mode == TorchMode.STROBE) {
                Spacer(Modifier.height(12.dp))
                Text(
                    String.format(Locale.US, "%.1f flashes per second", strobeHz),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = strobeHz,
                    onValueChange = { strobeHz = it },
                    valueRange = 1f..15f,
                )
            }
        }

        HintText(
            "SOS repeats the international distress signal: three short, three long, three " +
                "short. Long strobe sessions warm the LED, so the flash may dim until it cools." +
                "\n\nThis tool uses the system torch control and never opens the camera, so no " +
                "camera permission is requested here."
        )
    }
}
