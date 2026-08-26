package com.utilitybox.app.tools.measure

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utilitybox.app.ui.common.BigReadout
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CompassScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val rotationSensor = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val hasCompass = rotationSensor != null

    var heading by remember { mutableFloatStateOf(0f) }
    var smoothed by remember { mutableFloatStateOf(0f) }
    var accuracy by remember { mutableStateOf("Calibrating") }
    var tilt by remember { mutableFloatStateOf(0f) }

    DisposableEffect(rotationSensor) {
        if (rotationSensor == null) return@DisposableEffect onDispose { }
        val rotationMatrix = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val (axisX, axisY) = displayAxes(context)
                SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remapped)
                SensorManager.getOrientation(remapped, orientation)

                val degrees = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
                heading = degrees
                tilt = Math.toDegrees(orientation[1].toDouble()).toFloat()
                // Circular exponential smoothing keeps the needle steady near 0/360.
                smoothed = smoothAngle(smoothed, degrees, 0.15f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, value: Int) {
                accuracy = when (value) {
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low — move in a figure 8"
                    SensorManager.SENSOR_STATUS_UNRELIABLE -> "Unreliable — recalibrate"
                    else -> "Unknown"
                }
            }
        }
        sensorManager?.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    ToolScaffold(title = "Compass", onBack = onBack) {
        if (!hasCompass) {
            SectionCard {
                Text(
                    "This device has no magnetometer, so a magnetic heading cannot be measured.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@ToolScaffold
        }

        SectionCard {
            BigReadout(value = heading.roundToInt().toString().padStart(3, '0'), unit = "°")
            Spacer(Modifier.height(4.dp))
            Text(
                text = cardinalName(heading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            CompassRose(heading = smoothed)
        }

        SectionCard(title = "Details") {
            InfoRow("Heading", String.format(Locale.US, "%.1f°", heading))
            InfoRow("Direction", cardinalName(heading))
            InfoRow("Tilt", String.format(Locale.US, "%.1f°", tilt))
            InfoRow("Sensor accuracy", accuracy)
        }

        HintText(
            "Readings are magnetic north, not true north. Metal objects, magnets and cases " +
                "distort the field — if the needle drifts, wave the phone in a figure 8."
        )
    }
}

@Composable
private fun CompassRose(heading: Float) {
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val needleNorth = MaterialTheme.colorScheme.error
    val needleSouth = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val radius = size.minDimension / 2f * 0.88f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(color = outline, radius = radius, center = center, style = Stroke(width = 3f))

        // The dial rotates opposite to the heading so north always points north.
        rotate(degrees = -heading, pivot = center) {
            for (degree in 0 until 360 step 5) {
                val major = degree % 45 == 0
                val tickLength = if (major) radius * 0.14f else radius * 0.06f
                val angle = Math.toRadians(degree.toDouble() - 90.0)
                val outer = Offset(
                    center.x + (radius * cos(angle)).toFloat(),
                    center.y + (radius * sin(angle)).toFloat(),
                )
                val inner = Offset(
                    center.x + ((radius - tickLength) * cos(angle)).toFloat(),
                    center.y + ((radius - tickLength) * sin(angle)).toFloat(),
                )
                drawLine(
                    color = if (major) onSurface else muted,
                    start = inner,
                    end = outer,
                    strokeWidth = if (major) 4f else 2f,
                )
            }

            listOf(0 to "N", 90 to "E", 180 to "S", 270 to "W").forEach { (degree, label) ->
                val angle = Math.toRadians(degree.toDouble() - 90.0)
                val labelRadius = radius * 0.72f
                val layout = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (label == "N") needleNorth else onSurface,
                    ),
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        center.x + (labelRadius * cos(angle)).toFloat() - layout.size.width / 2f,
                        center.y + (labelRadius * sin(angle)).toFloat() - layout.size.height / 2f,
                    ),
                )
            }

            // Needle: red half points to magnetic north.
            val needleLength = radius * 0.62f
            val needleWidth = radius * 0.09f
            drawPath(
                path = Path().apply {
                    moveTo(center.x, center.y - needleLength)
                    lineTo(center.x - needleWidth, center.y)
                    lineTo(center.x + needleWidth, center.y)
                    close()
                },
                color = needleNorth,
            )
            drawPath(
                path = Path().apply {
                    moveTo(center.x, center.y + needleLength)
                    lineTo(center.x - needleWidth, center.y)
                    lineTo(center.x + needleWidth, center.y)
                    close()
                },
                color = needleSouth,
            )
        }

        drawCircle(color = accent, radius = radius * 0.06f, center = center)

        // Fixed pointer at the top marks the direction the phone is facing.
        drawPath(
            path = Path().apply {
                moveTo(center.x, center.y - radius - 6f)
                lineTo(center.x - 14f, center.y - radius - 30f)
                lineTo(center.x + 14f, center.y - radius - 30f)
                close()
            },
            color = accent,
        )
    }
}

/** Blends two angles the short way around the circle. */
private fun smoothAngle(current: Float, target: Float, factor: Float): Float {
    var delta = target - current
    while (delta > 180f) delta -= 360f
    while (delta < -180f) delta += 360f
    return (current + delta * factor + 360f) % 360f
}

private fun cardinalName(degrees: Float): String {
    val names = listOf(
        "North", "North-north-east", "North-east", "East-north-east",
        "East", "East-south-east", "South-east", "South-south-east",
        "South", "South-south-west", "South-west", "West-south-west",
        "West", "West-north-west", "North-west", "North-north-west",
    )
    val index = (((degrees + 11.25f) % 360f) / 22.5f).toInt().coerceIn(0, 15)
    return names[index]
}

/** Keeps the heading correct when the device is held in landscape. */
@Suppress("DEPRECATION")
private fun displayAxes(context: Context): Pair<Int, Int> {
    val rotation = runCatching {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        wm.defaultDisplay.rotation
    }.getOrDefault(Surface.ROTATION_0)

    return when (rotation) {
        Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
        Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
        Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
        else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
    }
}
