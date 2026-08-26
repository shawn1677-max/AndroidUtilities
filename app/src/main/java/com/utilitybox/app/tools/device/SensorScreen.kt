package com.utilitybox.app.tools.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.util.Locale

@Composable
fun SensorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val sensors = remember {
        sensorManager?.getSensorList(Sensor.TYPE_ALL)?.sortedBy { it.name } ?: emptyList()
    }
    var expanded by remember { mutableStateOf<String?>(null) }

    ToolScaffold(title = "Sensor Explorer", onBack = onBack) {
        HintText("${sensors.size} sensors reported by this device. Tap one to watch live values.")

        sensors.forEach { sensor ->
            val key = "${sensor.name}#${sensor.type}"
            SectionCard(
                modifier = Modifier.clickable {
                    expanded = if (expanded == key) null else key
                },
            ) {
                Text(sensor.name, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${typeName(sensor.type)} · ${sensor.vendor}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                AnimatedVisibility(visible = expanded == key) {
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(10.dp))
                        InfoRow("Type", sensor.stringType ?: typeName(sensor.type))
                        InfoRow("Version", sensor.version.toString())
                        InfoRow(
                            "Range",
                            String.format(Locale.US, "%.4f", sensor.maximumRange),
                        )
                        InfoRow(
                            "Resolution",
                            String.format(Locale.US, "%.6f", sensor.resolution),
                        )
                        InfoRow("Power", String.format(Locale.US, "%.2f mA", sensor.power))
                        InfoRow("Min delay", "${sensor.minDelay} µs")
                        InfoRow("Max delay", "${sensor.maxDelay} µs")
                        InfoRow("Wake-up sensor", if (sensor.isWakeUpSensor) "Yes" else "No")
                        Spacer(Modifier.height(10.dp))
                        LiveReading(sensorManager = sensorManager, sensor = sensor)
                    }
                }
            }
        }

        if (sensors.isEmpty()) {
            HintText("No sensors are available on this device.")
        }
    }
}

@Composable
private fun LiveReading(sensorManager: SensorManager?, sensor: Sensor) {
    var values by remember(sensor) { mutableStateOf(FloatArray(0)) }
    var accuracy by remember(sensor) { mutableStateOf("—") }

    DisposableEffect(sensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                values = event.values.copyOf()
            }

            override fun onAccuracyChanged(sensor: Sensor?, value: Int) {
                accuracy = accuracyName(value)
            }
        }
        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    Text(
        text = "Live values",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(4.dp))
    if (values.isEmpty()) {
        Text(
            "Waiting for data…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        values.forEachIndexed { index, value ->
            Text(
                text = "${axisLabel(sensor.type, index)}: ${String.format(Locale.US, "%+.5f", value)}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 1.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Accuracy: $accuracy",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun accuracyName(value: Int): String = when (value) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
    SensorManager.SENSOR_STATUS_UNRELIABLE -> "Unreliable"
    SensorManager.SENSOR_STATUS_NO_CONTACT -> "No contact"
    else -> "—"
}

private fun axisLabel(sensorType: Int, index: Int): String = when (sensorType) {
    Sensor.TYPE_ACCELEROMETER,
    Sensor.TYPE_GRAVITY,
    Sensor.TYPE_LINEAR_ACCELERATION,
    Sensor.TYPE_GYROSCOPE,
    Sensor.TYPE_MAGNETIC_FIELD,
    -> listOf("X", "Y", "Z").getOrElse(index) { "V$index" }

    Sensor.TYPE_ROTATION_VECTOR,
    Sensor.TYPE_GAME_ROTATION_VECTOR,
    -> listOf("X", "Y", "Z", "W", "Accuracy").getOrElse(index) { "V$index" }

    Sensor.TYPE_LIGHT -> "Illuminance (lx)"
    Sensor.TYPE_PRESSURE -> "Pressure (hPa)"
    Sensor.TYPE_PROXIMITY -> "Distance (cm)"
    Sensor.TYPE_AMBIENT_TEMPERATURE -> "Temperature (°C)"
    Sensor.TYPE_RELATIVE_HUMIDITY -> "Humidity (%)"
    Sensor.TYPE_STEP_COUNTER -> "Steps since boot"
    else -> "Value $index"
}

private fun typeName(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
    Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer"
    Sensor.TYPE_GYROSCOPE -> "Gyroscope"
    Sensor.TYPE_LIGHT -> "Light"
    Sensor.TYPE_PRESSURE -> "Barometer"
    Sensor.TYPE_PROXIMITY -> "Proximity"
    Sensor.TYPE_GRAVITY -> "Gravity"
    Sensor.TYPE_LINEAR_ACCELERATION -> "Linear acceleration"
    Sensor.TYPE_ROTATION_VECTOR -> "Rotation vector"
    Sensor.TYPE_RELATIVE_HUMIDITY -> "Humidity"
    Sensor.TYPE_AMBIENT_TEMPERATURE -> "Thermometer"
    Sensor.TYPE_STEP_COUNTER -> "Step counter"
    Sensor.TYPE_STEP_DETECTOR -> "Step detector"
    Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game rotation vector"
    Sensor.TYPE_SIGNIFICANT_MOTION -> "Significant motion"
    else -> "Type $type"
}
