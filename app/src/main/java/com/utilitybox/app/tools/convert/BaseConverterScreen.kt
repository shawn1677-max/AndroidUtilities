package com.utilitybox.app.tools.convert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.CopyableResult
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.math.BigInteger

@Composable
fun BaseConverterScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("255") }
    var inputBase by remember { mutableIntStateOf(10) }
    var customBase by remember { mutableIntStateOf(36) }

    val parsed = remember(input, inputBase) { parseInBase(input, inputBase) }

    ToolScaffold(title = "Number Base", onBack = onBack) {
        SectionCard(title = "Input") {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Value in base $inputBase") },
                singleLine = true,
                isError = input.isNotBlank() && parsed == null,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 8, 10, 16).forEach { base ->
                    FilterChip(
                        selected = inputBase == base,
                        onClick = { inputBase = base },
                        label = { Text(baseName(base)) },
                    )
                }
            }
            if (input.isNotBlank() && parsed == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Not a valid base-$inputBase number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        CopyableResult("Binary (base 2)", parsed?.toString(2)?.grouped(4) ?: "—")
        CopyableResult("Octal (base 8)", parsed?.toString(8) ?: "—")
        CopyableResult("Decimal (base 10)", parsed?.toString(10) ?: "—")
        CopyableResult("Hexadecimal (base 16)", parsed?.toString(16)?.uppercase() ?: "—")
        CopyableResult("Base $customBase", parsed?.toString(customBase)?.uppercase() ?: "—")

        SectionCard(title = "Custom base") {
            Text("Base $customBase", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = customBase.toFloat(),
                onValueChange = { customBase = it.toInt().coerceIn(2, 36) },
                valueRange = 2f..36f,
                steps = 33,
            )
        }

        if (parsed != null && parsed.signum() >= 0 && parsed.bitLength() <= 64) {
            SectionCard(title = "Bit view") {
                val bits = parsed.toString(2).padStart(if (parsed.bitLength() <= 32) 32 else 64, '0')
                Text(
                    text = bits.grouped(8),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bit length: ${parsed.bitLength()} · Set bits: ${parsed.bitCount()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HintText(
            "Arbitrary precision: values far beyond 64 bits convert exactly. Digits above 9 " +
                "use the letters A-Z, so base 36 uses 0-9 then A-Z."
        )
    }
}

private fun parseInBase(text: String, base: Int): BigInteger? {
    val cleaned = text.trim()
        .removePrefix("0x").removePrefix("0X")
        .removePrefix("0b").removePrefix("0B")
        .replace("_", "")
        .replace(" ", "")
    if (cleaned.isEmpty()) return null
    return runCatching { BigInteger(cleaned, base) }.getOrNull()
}

private fun baseName(base: Int): String = when (base) {
    2 -> "Binary"
    8 -> "Octal"
    10 -> "Decimal"
    16 -> "Hex"
    else -> "Base $base"
}

/** Inserts a space every [size] characters counting from the right. */
private fun String.grouped(size: Int): String =
    reversed().chunked(size).joinToString(" ").reversed()
