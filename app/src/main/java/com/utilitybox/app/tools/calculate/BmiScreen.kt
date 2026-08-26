package com.utilitybox.app.tools.calculate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.utilitybox.app.ui.common.BigReadout
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.util.Locale

@Composable
fun BmiScreen(onBack: () -> Unit) {
    var metric by remember { mutableStateOf(true) }
    var weight by remember { mutableStateOf("") }
    var heightPrimary by remember { mutableStateOf("") }
    var heightInches by remember { mutableStateOf("") }

    val weightValue = weight.replace(',', '.').toDoubleOrNull()
    val heightValue = heightPrimary.replace(',', '.').toDoubleOrNull()
    val inchesValue = heightInches.replace(',', '.').toDoubleOrNull() ?: 0.0

    val heightMetres = when {
        heightValue == null -> null
        metric -> heightValue / 100.0
        else -> (heightValue * 12 + inchesValue) * 0.0254
    }
    val weightKg = when {
        weightValue == null -> null
        metric -> weightValue
        else -> weightValue * 0.45359237
    }

    val bmi = if (weightKg != null && heightMetres != null && heightMetres > 0) {
        weightKg / (heightMetres * heightMetres)
    } else {
        null
    }

    ToolScaffold(title = "BMI Calculator", onBack = onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = metric,
                onClick = { metric = true },
                label = { Text("Metric") },
            )
            FilterChip(
                selected = !metric,
                onClick = { metric = false },
                label = { Text("Imperial") },
            )
        }

        SectionCard {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (metric) "Weight (kg)" else "Weight (lb)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Spacer(Modifier.height(10.dp))
            if (metric) {
                OutlinedTextField(
                    value = heightPrimary,
                    onValueChange = { heightPrimary = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Height (cm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = heightPrimary,
                        onValueChange = { heightPrimary = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Feet") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        value = heightInches,
                        onValueChange = { heightInches = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Inches") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            }
        }

        SectionCard {
            BigReadout(value = bmi?.let { String.format(Locale.US, "%.1f", it) } ?: "—")
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { ((bmi ?: 0.0) / 40.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = bmi?.let { category(it) } ?: "Enter your height and weight",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (bmi != null && heightMetres != null) {
            SectionCard(title = "For your height") {
                val lower = 18.5 * heightMetres * heightMetres
                val upper = 24.9 * heightMetres * heightMetres
                InfoRow(
                    "Healthy range",
                    if (metric) {
                        String.format(Locale.US, "%.1f – %.1f kg", lower, upper)
                    } else {
                        String.format(
                            Locale.US, "%.1f – %.1f lb",
                            lower / 0.45359237, upper / 0.45359237,
                        )
                    },
                )
                InfoRow("Underweight below", String.format(Locale.US, "%.1f", 18.5))
                InfoRow("Overweight above", String.format(Locale.US, "%.1f", 24.9))
            }
        }

        SectionCard(title = "Categories") {
            InfoRow("Underweight", "Below 18.5", copyable = false)
            InfoRow("Healthy weight", "18.5 – 24.9", copyable = false)
            InfoRow("Overweight", "25.0 – 29.9", copyable = false)
            InfoRow("Obese", "30.0 and above", copyable = false)
        }

        HintText(
            "BMI is a rough population-level screening figure, not a diagnosis. It does not " +
                "distinguish muscle from fat and is less meaningful for athletes, children, " +
                "older adults and during pregnancy. Talk to a clinician about what your " +
                "number means for you."
        )
    }
}

private fun category(bmi: Double): String = when {
    bmi < 16 -> "Severely underweight"
    bmi < 18.5 -> "Underweight"
    bmi < 25 -> "Healthy weight"
    bmi < 30 -> "Overweight"
    bmi < 35 -> "Obese (class I)"
    bmi < 40 -> "Obese (class II)"
    else -> "Obese (class III)"
}
