package com.utilitybox.app.tools.convert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@Composable
fun UnitConverterScreen(onBack: () -> Unit) {
    var categoryIndex by remember { mutableIntStateOf(0) }
    var fromIndex by remember { mutableIntStateOf(0) }
    var toIndex by remember { mutableIntStateOf(1) }
    var input by remember { mutableStateOf("1") }

    val category = UnitCatalog.categories[categoryIndex]
    val from = category.units[fromIndex.coerceIn(category.units.indices)]
    val to = category.units[toIndex.coerceIn(category.units.indices)]

    val value = input.replace(',', '.').toDoubleOrNull()
    val converted = value?.let { to.fromBase(from.toBase(it)) }

    ToolScaffold(title = "Unit Converter", onBack = onBack) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(UnitCatalog.categories.size) { index ->
                FilterChip(
                    selected = categoryIndex == index,
                    onClick = {
                        categoryIndex = index
                        fromIndex = 0
                        toIndex = 1
                    },
                    label = { Text(UnitCatalog.categories[index].name) },
                )
            }
        }

        SectionCard {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Value") },
                singleLine = true,
                isError = input.isNotBlank() && value == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text(from.symbol) },
            )

            Spacer(Modifier.height(12.dp))
            UnitPicker(
                label = "From",
                units = category.units,
                selectedIndex = fromIndex,
                onSelect = { fromIndex = it },
            )

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                OutlinedButton(onClick = {
                    val previousFrom = fromIndex
                    fromIndex = toIndex
                    toIndex = previousFrom
                }) {
                    Icon(Icons.Outlined.SwapVert, contentDescription = null)
                    Spacer(Modifier.height(4.dp))
                    Text(" Swap")
                }
            }
            Spacer(Modifier.height(8.dp))

            UnitPicker(
                label = "To",
                units = category.units,
                selectedIndex = toIndex,
                onSelect = { toIndex = it },
            )
        }

        CopyableResult(
            label = "${from.name} → ${to.name}",
            value = converted?.let { "${formatResult(it)} ${to.symbol}" } ?: "—",
        )

        if (value != null) {
            SectionCard(title = "All units in ${category.name}") {
                val base = from.toBase(value)
                category.units.forEach { unit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(unit.name, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        Text(
                            "${formatResult(unit.fromBase(base))} ${unit.symbol}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        HintText(
            "Digital storage lists both decimal units (kB, MB) and binary units (KiB, MiB). " +
                "Drive manufacturers use decimal; operating systems usually report binary."
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitPicker(
    label: String,
    units: List<UnitDef>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = units[selectedIndex.coerceIn(units.indices)]

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = "${selected.name} (${selected.symbol})",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEachIndexed { index, unit ->
                DropdownMenuItem(
                    text = { Text("${unit.name} (${unit.symbol})") },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Keeps significant digits for very small and very large results without scientific noise. */
private fun formatResult(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    if (value == 0.0) return "0"
    val magnitude = kotlin.math.abs(value)
    return when {
        magnitude >= 1e12 || magnitude < 1e-6 -> String.format(java.util.Locale.US, "%.6g", value)
        else -> BigDecimal(value)
            .round(MathContext(10))
            .setScale(6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }
}
