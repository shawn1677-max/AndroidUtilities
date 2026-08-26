package com.utilitybox.app.tools.calculate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private enum class DateMode(val label: String) {
    DIFFERENCE("Between two dates"),
    ADD("Add or subtract"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateCalculatorScreen(onBack: () -> Unit) {
    var mode by remember { mutableStateOf(DateMode.DIFFERENCE) }
    var start by remember { mutableStateOf(LocalDate.now()) }
    var end by remember { mutableStateOf(LocalDate.now().plusDays(30)) }
    var amount by remember { mutableStateOf("30") }
    var unit by remember { mutableStateOf(ChronoUnit.DAYS) }
    var subtract by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<String?>(null) }

    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault())
    }

    ToolScaffold(title = "Date Calculator", onBack = onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateMode.entries.forEach { option ->
                FilterChip(
                    selected = mode == option,
                    onClick = { mode = option },
                    label = { Text(option.label) },
                )
            }
        }

        SectionCard(title = if (mode == DateMode.DIFFERENCE) "Dates" else "Start date") {
            OutlinedButton(
                onClick = { picking = "start" },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(start.format(formatter)) }

            if (mode == DateMode.DIFFERENCE) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { picking = "end" },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(end.format(formatter)) }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { start = LocalDate.now() }) { Text("Start = today") }
                    TextButton(onClick = { end = LocalDate.now() }) { Text("End = today") }
                }
            }
        }

        if (mode == DateMode.ADD) {
            SectionCard(title = "Amount") {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { character -> character.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("How many") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ChronoUnit.DAYS to "Days",
                        ChronoUnit.WEEKS to "Weeks",
                        ChronoUnit.MONTHS to "Months",
                        ChronoUnit.YEARS to "Years",
                    ).forEach { (option, label) ->
                        FilterChip(
                            selected = unit == option,
                            onClick = { unit = option },
                            label = { Text(label) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !subtract,
                        onClick = { subtract = false },
                        label = { Text("Add") },
                    )
                    FilterChip(
                        selected = subtract,
                        onClick = { subtract = true },
                        label = { Text("Subtract") },
                    )
                }
            }
        }

        when (mode) {
            DateMode.DIFFERENCE -> {
                val from = minOf(start, end)
                val to = maxOf(start, end)
                val period = Period.between(from, to)
                val totalDays = ChronoUnit.DAYS.between(from, to)

                CopyableResult("Total days", "$totalDays", monospace = false)
                SectionCard(title = "Breakdown") {
                    InfoRow(
                        "Calendar",
                        "${period.years} years, ${period.months} months, ${period.days} days",
                    )
                    InfoRow("Weeks", "${totalDays / 7} weeks and ${totalDays % 7} days")
                    InfoRow("Months", "${ChronoUnit.MONTHS.between(from, to)}")
                    InfoRow("Working days", "${workingDays(from, to)}")
                    InfoRow("Hours", "${totalDays * 24}")
                }
            }

            DateMode.ADD -> {
                val count = amount.toLongOrNull() ?: 0L
                val signed = if (subtract) -count else count
                val result = start.plus(signed, unit)
                CopyableResult("Result", result.format(formatter), monospace = false)
                SectionCard(title = "Details") {
                    InfoRow("ISO date", result.toString())
                    InfoRow("Day of week", result.dayOfWeek.getDisplayName(
                        java.time.format.TextStyle.FULL, Locale.getDefault()
                    ))
                    InfoRow("Day of year", "${result.dayOfYear}")
                    InfoRow("Week of year", "${result.get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())}")
                    InfoRow("Leap year", if (result.isLeapYear) "Yes" else "No")
                }
            }
        }

        HintText(
            "Working days count Monday to Friday and ignore public holidays, which vary by " +
                "country."
        )

        val target = picking
        if (target != null) {
            val initial = if (target == "start") start else end
            val state = rememberDatePickerState(
                initialSelectedDateMillis = initial
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { picking = null },
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            if (target == "start") start = picked else end = picked
                        }
                        picking = null
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { picking = null }) { Text("Cancel") }
                },
            ) {
                DatePicker(state = state)
            }
        }
    }
}

private fun workingDays(from: LocalDate, to: LocalDate): Long {
    var count = 0L
    var cursor = from
    while (cursor.isBefore(to)) {
        val day = cursor.dayOfWeek.value
        if (day <= 5) count++
        cursor = cursor.plusDays(1)
    }
    return count
}
