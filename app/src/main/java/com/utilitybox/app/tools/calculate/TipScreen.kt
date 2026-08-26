package com.utilitybox.app.tools.calculate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.CopyableResult
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.util.Locale
import kotlin.math.ceil

@Composable
fun TipScreen(onBack: () -> Unit) {
    var billText by remember { mutableStateOf("") }
    var tipPercent by remember { mutableFloatStateOf(15f) }
    var people by remember { mutableIntStateOf(1) }
    var roundUp by remember { mutableStateOf(false) }

    val bill = billText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val rawTip = bill * tipPercent / 100.0
    val rawTotal = bill + rawTip
    val total = if (roundUp) ceil(rawTotal) else rawTotal
    val tip = total - bill
    val perPerson = if (people > 0) total / people else total

    ToolScaffold(title = "Tip Calculator", onBack = onBack) {
        SectionCard {
            OutlinedTextField(
                value = billText,
                onValueChange = { billText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bill amount") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            Spacer(Modifier.height(16.dp))
            Text(
                "Tip: ${String.format(Locale.US, "%.0f", tipPercent)}%",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = tipPercent,
                onValueChange = { tipPercent = it },
                valueRange = 0f..40f,
                steps = 39,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10f, 15f, 18f, 20f, 25f).forEach { preset ->
                    FilterChip(
                        selected = tipPercent == preset,
                        onClick = { tipPercent = preset },
                        label = { Text("${preset.toInt()}%") },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Split between $people", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { if (people > 1) people-- },
                        enabled = people > 1,
                    ) { Text("−") }
                    OutlinedButton(onClick = { if (people < 50) people++ }) { Text("+") }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Round the total up", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = roundUp, onCheckedChange = { roundUp = it })
            }
        }

        CopyableResult("Tip", money(tip), monospace = false)
        CopyableResult("Total", money(total), monospace = false)
        if (people > 1) {
            CopyableResult("Each person pays", money(perPerson), monospace = false)
        }

        HintText(
            "Rounding up adds the difference to the tip, which is the usual way to settle " +
                "on a round number. Amounts are shown without a currency symbol so they " +
                "work wherever you are."
        )
    }
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
