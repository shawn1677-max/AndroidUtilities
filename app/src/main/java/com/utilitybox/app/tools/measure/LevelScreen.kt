package com.utilitybox.app.tools.measure

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

private const val LEVEL_TOLERANCE_DEGREES = 0.6f

@Composable
fun LevelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val sensor = remember {
        sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    var pitch by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }

    DisposableEffect(sensor) {
        if (sensor == null) return@DisposableEffect onDispose { }
        val filtered = FloatArray(3)
        var seeded = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Low-pass filter so the bubble settles instead of jittering.
                if (!seeded) {
                    event.values.copyInto(filtered, 0, 0, 3)
                    seeded = true
                } else {
                    for (i in 0..2) {
                        filtered[i] = filtered[i] + 0.2f * (event.values[i] - filtered[i])
                    }
                }
                val (x, y, z) = Triple(filtered[0], filtered[1], filtered[2])
                pitch = Math.toDegrees(atan2(y.toDouble(), hypot(x.toDouble(), z.toDouble()))).toFloat()
                roll = Math.toDegrees(atan2(-x.toDouble(), hypot(y.toDouble(), z.toDouble()))).toFloat()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    val isLevel = abs(pitch) < LEVEL_TOLERANCE_DEGREES && abs(roll) < LEVEL_TOLERANCE_DEGREES

    ToolScaffold(title = "Bubble Level", onBack = onBack) {
        if (sensor == null) {
            SectionCard {
                Text(
                    "This device has no accelerometer, so tilt cannot be measured.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@ToolScaffold
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            BubbleView(pitch = pitch, roll = roll, isLevel = isLevel)
        }

        SectionCard {
            Text(
                text = if (isLevel) "Level" else "Not level",
                style = MaterialTheme.typography.titleMedium,
                color = if (isLevel) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(title = "Angles") {
            InfoRow("Pitch (front-back)", String.format(Locale.US, "%+.1f°", pitch))
            InfoRow("Roll (left-right)", String.format(Locale.US, "%+.1f°", roll))
            InfoRow(
                "Total tilt",
                String.format(Locale.US, "%.1f°", sqrt(pitch * pitch + roll * roll)),
            )
            InfoRow("Slope", String.format(Locale.US, "%.1f %%", slopePercent(pitch, roll)))
        }

        HintText(
            "Lay the phone flat on a surface to check a table or shelf, or stand it on its " +
                "edge against a wall to check plumb. Accuracy depends on the phone's own " +
                "flatness — a case or camera bump will bias the reading."
        )
    }
}

@Composable
private fun BubbleView(pitch: Float, roll: Float, isLevel: Boolean) {
    val outline = MaterialTheme.colorScheme.outline
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val bubbleColor = if (isLevel) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension / 2f * 0.9f
        val bubbleRadius = outerRadius * 0.16f

        drawCircle(outline, radius = outerRadius, center = center, style = Stroke(3f))
        drawCircle(muted, radius = outerRadius * 0.5f, center = center, style = Stroke(2f))
        drawCircle(
            color = bubbleColor,
            radius = outerRadius * 0.2f,
            center = center,
            style = Stroke(2f),
        )
        drawLine(muted, Offset(center.x - outerRadius, center.y), Offset(center.x + outerRadius, center.y), 1.5f)
        drawLine(muted, Offset(center.x, center.y - outerRadius), Offset(center.x, center.y + outerRadius), 1.5f)

        // Map ±30° of tilt across the dial, clamped so the bubble stays inside.
        val travel = outerRadius - bubbleRadius
        val maxAngle = 30f
        val dx = (roll / maxAngle).coerceIn(-1f, 1f) * travel
        val dy = (pitch / maxAngle).coerceIn(-1f, 1f) * travel
        val offset = Offset(center.x + dx, center.y + dy)
        val clamped = clampToCircle(offset, center, travel)

        drawCircle(color = bubbleColor.copy(alpha = 0.25f), radius = bubbleRadius, center = clamped)
        drawCircle(color = bubbleColor, radius = bubbleRadius, center = clamped, style = Stroke(3f))
    }
}

private fun clampToCircle(point: Offset, center: Offset, maxRadius: Float): Offset {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (distance <= maxRadius || distance == 0f) return point
    val scale = maxRadius / distance
    return Offset(center.x + dx * scale, center.y + dy * scale)
}

/** Builder's slope: rise over run as a percentage. */
private fun slopePercent(pitch: Float, roll: Float): Float {
    val tilt = sqrt(pitch * pitch + roll * roll)
    return (Math.tan(Math.toRadians(min(tilt, 89f).toDouble())) * 100).toFloat()
}
