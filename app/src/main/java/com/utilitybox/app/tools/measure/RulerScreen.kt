package com.utilitybox.app.tools.measure

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.edit
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.util.Locale

private const val PREFS = "utilitybox_ruler"
private const val KEY_CALIBRATION = "calibration"

@Composable
fun RulerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val displayMetrics = LocalResources.current.displayMetrics
    val reportedYdpi = remember(displayMetrics) {
        displayMetrics.ydpi.takeIf { it > 0f } ?: displayMetrics.densityDpi.toFloat()
    }
    var calibration by remember { mutableFloatStateOf(prefs.getFloat(KEY_CALIBRATION, 1f)) }
    var metric by remember { mutableStateOf(true) }

    val effectiveDpi = reportedYdpi * calibration

    ToolScaffold(title = "Screen Ruler", onBack = onBack, scrollable = false) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = metric,
                    onClick = { metric = true },
                    label = { Text("Centimetres") },
                )
                FilterChip(
                    selected = !metric,
                    onClick = { metric = false },
                    label = { Text("Inches") },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                RulerCanvas(dpi = effectiveDpi, metric = metric)
            }

            SectionCard(
                title = "Calibration",
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = String.format(
                        Locale.US,
                        "%.0f dpi (%.0f%% of reported)",
                        effectiveDpi,
                        calibration * 100,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = calibration,
                    onValueChange = { calibration = it },
                    onValueChangeFinished = {
                        prefs.edit { putFloat(KEY_CALIBRATION, calibration) }
                    },
                    valueRange = 0.7f..1.3f,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        calibration = 1f
                        prefs.edit { putFloat(KEY_CALIBRATION, 1f) }
                    }) { Text("Reset") }
                }
                Spacer(Modifier.height(4.dp))
                HintText(
                    "Hold a bank card against the ruler — it is exactly 8.56 cm (3.37 in) " +
                        "long — and adjust the slider until the marks line up. The setting is " +
                        "remembered."
                )
            }
        }
    }
}

@Composable
private fun RulerCanvas(dpi: Float, metric: Boolean) {
    val line = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurface

    Canvas(Modifier.fillMaxSize()) {
        val pixelsPerUnit = if (metric) dpi / 2.54f else dpi
        val subdivisions = if (metric) 10 else 16
        val pixelsPerTick = pixelsPerUnit / subdivisions
        if (pixelsPerTick <= 0.5f) return@Canvas

        val totalTicks = (size.height / pixelsPerTick).toInt()

        for (tick in 0..totalTicks) {
            val y = tick * pixelsPerTick
            val isWhole = tick % subdivisions == 0
            val isHalf = tick % (subdivisions / 2) == 0
            val isQuarter = !metric && tick % 4 == 0

            val length = when {
                isWhole -> size.width * 0.42f
                isHalf -> size.width * 0.28f
                isQuarter -> size.width * 0.2f
                else -> size.width * 0.12f
            }

            drawLine(
                color = if (isWhole) accent else line,
                start = Offset(0f, y),
                end = Offset(length, y),
                strokeWidth = if (isWhole) 3f else 1.5f,
            )

            if (isWhole && tick > 0) {
                val layout = textMeasurer.measure(
                    text = "${tick / subdivisions}",
                    style = TextStyle(fontSize = 16.sp, color = labelColor),
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(length + 10f, y - layout.size.height / 2f),
                )
            }
        }

        val unitLabel = textMeasurer.measure(
            text = if (metric) "cm" else "in",
            style = TextStyle(fontSize = 14.sp, color = labelColor),
        )
        drawText(
            textLayoutResult = unitLabel,
            topLeft = Offset(size.width * 0.42f + 10f, 8f),
        )
    }
}
