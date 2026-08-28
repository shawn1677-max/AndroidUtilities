package com.utilitybox.app.tools.measure

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.BigReadout
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.util.Locale
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max

@Composable
fun LightMeterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val sensor = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT) }

    var lux by remember { mutableFloatStateOf(0f) }
    var peak by remember { mutableFloatStateOf(0f) }
    var seen by remember { mutableStateOf(false) }

    DisposableEffect(sensor) {
        if (sensor == null) return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                lux = event.values[0]
                if (lux > peak) peak = lux
                seen = true
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    ToolScaffold(title = "Light Meter", onBack = onBack) {
        if (sensor == null) {
            SectionCard {
                Text(
                    "This device has no ambient light sensor.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@ToolScaffold
        }

        SectionCard {
            BigReadout(
                value = if (!seen) "—" else String.format(Locale.US, "%,.0f", lux),
                unit = "lux",
            )
            Spacer(Modifier.height(10.dp))
            // Logarithmic, because human brightness perception is: a linear bar
            // would sit pinned at either end almost everywhere.
            LinearProgressIndicator(
                progress = { logScale(lux) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (seen) describe(lux) else "Waiting for the sensor…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(title = "Details") {
            InfoRow("Illuminance", if (seen) String.format(Locale.US, "%.1f lx", lux) else "—")
            InfoRow(
                "Foot-candles",
                if (seen) String.format(Locale.US, "%.2f fc", lux / 10.7639) else "—",
            )
            InfoRow(
                "Exposure value at ISO 100",
                if (seen && lux > 0) String.format(Locale.US, "EV %.1f", exposureValue(lux)) else "—",
            )
            InfoRow("Brightest seen", if (seen) String.format(Locale.US, "%,.0f lx", peak) else "—")
            InfoRow("Sensor range", String.format(Locale.US, "%,.0f lx", sensor.maximumRange))
        }

        HintText(
            "The sensor sits near the earpiece, so point that part of the phone at what " +
                "you are measuring and keep your hand clear. Phone light sensors are " +
                "coarse and are tuned for adjusting screen brightness, so treat the " +
                "exposure value as a starting point rather than a meter reading."
        )
    }
}

/** Maps 0–100,000 lux onto 0–1 logarithmically. */
internal fun logScale(lux: Float): Float {
    if (lux <= 0f) return 0f
    return (ln(lux.toDouble() + 1) / ln(100_001.0)).toFloat().coerceIn(0f, 1f)
}

/** EV at ISO 100 from illuminance, using the common C = 250 incident constant. */
internal fun exposureValue(lux: Float): Double = log2(max(lux, 0.001f).toDouble() * 100.0 / 250.0)

private fun describe(lux: Float): String = when {
    lux < 1 -> "Effectively dark — moonlight or less"
    lux < 50 -> "Very dim — a corridor at night"
    lux < 200 -> "Dim indoor lighting"
    lux < 500 -> "Comfortable indoor lighting"
    lux < 1_000 -> "Bright indoors — good for reading and detailed work"
    lux < 10_000 -> "Overcast daylight"
    lux < 25_000 -> "Bright daylight in shade"
    else -> "Direct sunlight"
}
