package com.utilitybox.app.tools.measure

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

@Composable
fun BarometerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val sensor = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE) }

    var pressure by remember { mutableFloatStateOf(0f) }
    var seaLevel by remember {
        mutableFloatStateOf(SensorManager.PRESSURE_STANDARD_ATMOSPHERE)
    }
    var hasReading by remember { mutableFloatStateOf(0f) }

    DisposableEffect(sensor) {
        if (sensor == null) return@DisposableEffect onDispose { }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Light smoothing: the raw signal jitters by a few tenths of a hPa.
                pressure = if (hasReading == 0f) {
                    event.values[0]
                } else {
                    pressure + 0.2f * (event.values[0] - pressure)
                }
                hasReading = 1f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    ToolScaffold(title = "Barometer", onBack = onBack) {
        if (sensor == null) {
            SectionCard {
                Text(
                    "This device has no pressure sensor. Barometers are common on flagship " +
                        "phones and rare on budget ones.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@ToolScaffold
        }

        SectionCard {
            BigReadout(
                value = if (hasReading == 0f) "—" else String.format(Locale.US, "%.1f", pressure),
                unit = "hPa",
            )
        }

        SectionCard(title = "Readings") {
            InfoRow(
                "Pressure",
                if (hasReading == 0f) "—" else String.format(Locale.US, "%.2f hPa", pressure),
            )
            InfoRow(
                "In inches of mercury",
                if (hasReading == 0f) "—" else String.format(Locale.US, "%.2f inHg", pressure / 33.8639),
            )
            InfoRow(
                "Estimated altitude",
                if (hasReading == 0f) {
                    "—"
                } else {
                    String.format(
                        Locale.US,
                        "%.0f m  (%.0f ft)",
                        SensorManager.getAltitude(seaLevel, pressure),
                        SensorManager.getAltitude(seaLevel, pressure) * 3.28084,
                    )
                },
            )
            InfoRow("Weather hint", pressureTrendHint(pressure))
        }

        SectionCard(title = "Sea level reference") {
            Text(
                String.format(Locale.US, "%.1f hPa", seaLevel),
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = seaLevel,
                onValueChange = { seaLevel = it },
                valueRange = 950f..1070f,
            )
            TextButton(onClick = {
                seaLevel = SensorManager.PRESSURE_STANDARD_ATMOSPHERE
            }) { Text("Reset to standard (1013.25)") }
            Spacer(Modifier.height(4.dp))
            HintText(
                "Altitude from pressure is only as good as this reference. Standard " +
                    "atmosphere is a fair guess, but for a real figure set it to the " +
                    "current sea-level pressure from a local forecast."
            )
        }
    }
}

private fun pressureTrendHint(pressure: Float): String = when {
    pressure <= 0f -> "—"
    pressure < 1000 -> "Low — unsettled or stormy conditions are typical"
    pressure < 1013 -> "Slightly below average"
    pressure < 1023 -> "Around average"
    else -> "High — settled, fair conditions are typical"
}
