package com.utilitybox.app.tools.convert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.CopyableResult
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ColorToolScreen(onBack: () -> Unit) {
    var red by remember { mutableFloatStateOf(30f) }
    var green by remember { mutableFloatStateOf(111f) }
    var blue by remember { mutableFloatStateOf(92f) }
    var hexInput by remember { mutableStateOf("1E6F5C") }

    val r = red.roundToInt()
    val g = green.roundToInt()
    val b = blue.roundToInt()
    val hex = String.format(Locale.US, "%02X%02X%02X", r, g, b)
    val hsl = rgbToHsl(r, g, b)
    val colour = Color(r, g, b)

    ToolScaffold(title = "Colour Tool", onBack = onBack) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(colour, RoundedCornerShape(16.dp)),
        )

        SectionCard(title = "Channels") {
            ChannelSlider("Red", red, Color.Red) { red = it; hexInput = updatedHex(it, green, blue) }
            ChannelSlider("Green", green, Color.Green) { green = it; hexInput = updatedHex(red, it, blue) }
            ChannelSlider("Blue", blue, Color.Blue) { blue = it; hexInput = updatedHex(red, green, it) }
        }

        SectionCard(title = "Enter a hex value") {
            OutlinedTextField(
                value = hexInput,
                onValueChange = { text ->
                    hexInput = text.uppercase().filter { it.isDigit() || it in 'A'..'F' || it == '#' }
                    parseHex(hexInput)?.let { (pr, pg, pb) ->
                        red = pr.toFloat()
                        green = pg.toFloat()
                        blue = pb.toFloat()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hex (RGB or RRGGBB)") },
                singleLine = true,
                prefix = { Text("#") },
            )
        }

        CopyableResult("HEX", "#$hex")
        CopyableResult("RGB", "rgb($r, $g, $b)")
        CopyableResult(
            "HSL",
            String.format(
                Locale.US,
                "hsl(%.0f, %.0f%%, %.0f%%)",
                hsl[0], hsl[1] * 100, hsl[2] * 100,
            ),
        )
        CopyableResult("Android colour int", "0xFF$hex")
        CopyableResult(
            "CMYK",
            rgbToCmyk(r, g, b).let {
                String.format(Locale.US, "cmyk(%.0f%%, %.0f%%, %.0f%%, %.0f%%)", it[0], it[1], it[2], it[3])
            },
        )

        SectionCard(title = "Contrast") {
            val luminance = relativeLuminance(r, g, b)
            val onWhite = contrastRatio(luminance, 1.0)
            val onBlack = contrastRatio(luminance, 0.0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(
                        String.format(Locale.US, "%.2f:1", onWhite),
                        color = colour,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp)),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(
                        String.format(Locale.US, "%.2f:1", onBlack),
                        color = colour,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "WCAG AA needs 4.5:1 for body text and 3:1 for large text. " +
                    "Best pairing here: ${if (onWhite > onBlack) "white" else "black"} background.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HintText("Slide the channels or paste a hex value — the two stay in sync.")
    }
}

@Composable
private fun ChannelSlider(label: String, value: Float, tint: Color, onChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            "$label ${value.roundToInt()}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.size(width = 76.dp, height = 20.dp),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(thumbColor = tint, activeTrackColor = tint),
            modifier = Modifier.weight(1f),
        )
    }
}

private fun updatedHex(r: Float, g: Float, b: Float): String =
    String.format(Locale.US, "%02X%02X%02X", r.roundToInt(), g.roundToInt(), b.roundToInt())

private fun parseHex(text: String): Triple<Int, Int, Int>? {
    val clean = text.removePrefix("#")
    val expanded = when (clean.length) {
        3 -> clean.map { "$it$it" }.joinToString("")
        6 -> clean
        else -> return null
    }
    return runCatching {
        Triple(
            expanded.substring(0, 2).toInt(16),
            expanded.substring(2, 4).toInt(16),
            expanded.substring(4, 6).toInt(16),
        )
    }.getOrNull()
}

private fun rgbToHsl(r: Int, g: Int, b: Int): FloatArray {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val maxValue = max(rf, max(gf, bf))
    val minValue = min(rf, min(gf, bf))
    val delta = maxValue - minValue
    val lightness = (maxValue + minValue) / 2f

    if (delta == 0f) return floatArrayOf(0f, 0f, lightness)

    val saturation = delta / (1f - abs(2f * lightness - 1f))
    val hue = when (maxValue) {
        rf -> 60f * (((gf - bf) / delta) % 6f)
        gf -> 60f * (((bf - rf) / delta) + 2f)
        else -> 60f * (((rf - gf) / delta) + 4f)
    }
    return floatArrayOf((hue + 360f) % 360f, saturation.coerceIn(0f, 1f), lightness)
}

private fun rgbToCmyk(r: Int, g: Int, b: Int): DoubleArray {
    val rf = r / 255.0
    val gf = g / 255.0
    val bf = b / 255.0
    val k = 1.0 - max(rf, max(gf, bf))
    if (k >= 1.0) return doubleArrayOf(0.0, 0.0, 0.0, 100.0)
    return doubleArrayOf(
        (1.0 - rf - k) / (1.0 - k) * 100,
        (1.0 - gf - k) / (1.0 - k) * 100,
        (1.0 - bf - k) / (1.0 - k) * 100,
        k * 100,
    )
}

/** WCAG 2.1 relative luminance. */
private fun relativeLuminance(r: Int, g: Int, b: Int): Double {
    fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
}

private fun contrastRatio(a: Double, b: Double): Double {
    val lighter = max(a, b)
    val darker = min(a, b)
    return (lighter + 0.05) / (darker + 0.05)
}
