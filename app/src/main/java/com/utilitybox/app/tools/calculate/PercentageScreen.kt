package com.utilitybox.app.tools.calculate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.CopyableResult
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.formatNumber

private enum class PercentMode(val label: String) {
    PERCENT_OF("% of a number"),
    WHAT_PERCENT("X is what % of Y"),
    CHANGE("Change from X to Y"),
    DISCOUNT("Discount"),
    ADD_TAX("Add a percentage"),
}

@Composable
fun PercentageScreen(onBack: () -> Unit) {
    var mode by remember { mutableStateOf(PercentMode.PERCENT_OF) }
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }

    val a = first.replace(',', '.').toDoubleOrNull()
    val b = second.replace(',', '.').toDoubleOrNull()

    val labels = when (mode) {
        PercentMode.PERCENT_OF -> "Percentage" to "Number"
        PercentMode.WHAT_PERCENT -> "Value X" to "Total Y"
        PercentMode.CHANGE -> "From X" to "To Y"
        PercentMode.DISCOUNT -> "Original price" to "Discount %"
        PercentMode.ADD_TAX -> "Amount" to "Percentage to add"
    }

    ToolScaffold(title = "Percentages", onBack = onBack) {
        SectionCard(title = "Calculation") {
            PercentMode.entries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { option ->
                        FilterChip(
                            selected = mode == option,
                            onClick = { mode = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        SectionCard {
            OutlinedTextField(
                value = first,
                onValueChange = { first = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(labels.first) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = second,
                onValueChange = { second = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(labels.second) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }

        when (mode) {
            PercentMode.PERCENT_OF -> {
                val result = if (a != null && b != null) a / 100 * b else null
                CopyableResult(
                    "${first.orDash()}% of ${second.orDash()}",
                    result?.let { formatNumber(it, 6) } ?: "—",
                )
            }

            PercentMode.WHAT_PERCENT -> {
                val result = if (a != null && b != null && b != 0.0) a / b * 100 else null
                CopyableResult("Percentage", result?.let { "${formatNumber(it, 4)}%" } ?: "—")
            }

            PercentMode.CHANGE -> {
                val result = if (a != null && b != null && a != 0.0) (b - a) / a * 100 else null
                CopyableResult(
                    "Change",
                    result?.let {
                        val direction = if (it >= 0) "increase" else "decrease"
                        "${formatNumber(kotlin.math.abs(it), 4)}% $direction"
                    } ?: "—",
                )
                CopyableResult(
                    "Difference",
                    if (a != null && b != null) formatNumber(b - a, 6) else "—",
                )
            }

            PercentMode.DISCOUNT -> {
                val saved = if (a != null && b != null) a * b / 100 else null
                CopyableResult("You save", saved?.let { formatNumber(it, 2) } ?: "—")
                CopyableResult(
                    "Final price",
                    if (a != null && saved != null) formatNumber(a - saved, 2) else "—",
                )
            }

            PercentMode.ADD_TAX -> {
                val added = if (a != null && b != null) a * b / 100 else null
                CopyableResult("Added", added?.let { formatNumber(it, 2) } ?: "—")
                CopyableResult(
                    "New total",
                    if (a != null && added != null) formatNumber(a + added, 2) else "—",
                )
            }
        }

        HintText(
            "Percentage change is always measured against the first value, which is why a " +
                "50% fall needs a 100% rise to get back to where it started."
        )
    }
}

private fun String.orDash(): String = ifBlank { "—" }
