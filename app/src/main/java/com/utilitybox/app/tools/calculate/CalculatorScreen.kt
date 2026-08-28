package com.utilitybox.app.tools.calculate

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.CopyableResult
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.EvalResult
import com.utilitybox.app.util.Expressions
import com.utilitybox.app.util.formatCalculatorResult

private data class HistoryEntry(val expression: String, val result: String)

@Composable
fun CalculatorScreen(onBack: () -> Unit) {
    var expression by rememberSaveable { mutableStateOf("") }
    val history = remember { mutableStateListOf<HistoryEntry>() }

    val evaluation = remember(expression) { Expressions.evaluate(expression) }
    val preview = when (evaluation) {
        is EvalResult.Ok -> formatCalculatorResult(evaluation.value)
        is EvalResult.Error -> evaluation.message
    }
    val isError = evaluation is EvalResult.Error && preview.isNotEmpty()

    fun append(text: String) {
        expression += text
    }

    fun evaluateNow() {
        val result = evaluation
        if (result is EvalResult.Ok) {
            val formatted = formatCalculatorResult(result.value)
            history.add(0, HistoryEntry(expression, formatted))
            while (history.size > 8) history.removeAt(history.lastIndex)
            expression = formatted
        }
    }

    ToolScaffold(title = "Calculator", onBack = onBack) {
        SectionCard {
            Text(
                text = expression.ifEmpty { "0" },
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = preview,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        KeypadRow {
            KeyButton("C", Modifier.weight(1f), tonal = true) { expression = "" }
            KeyButton("⌫", Modifier.weight(1f), tonal = true) {
                if (expression.isNotEmpty()) expression = expression.dropLast(1)
            }
            KeyButton("(", Modifier.weight(1f), tonal = true) { append("(") }
            KeyButton(")", Modifier.weight(1f), tonal = true) { append(")") }
        }
        KeypadRow {
            KeyButton("7", Modifier.weight(1f)) { append("7") }
            KeyButton("8", Modifier.weight(1f)) { append("8") }
            KeyButton("9", Modifier.weight(1f)) { append("9") }
            KeyButton("÷", Modifier.weight(1f), tonal = true) { append("÷") }
        }
        KeypadRow {
            KeyButton("4", Modifier.weight(1f)) { append("4") }
            KeyButton("5", Modifier.weight(1f)) { append("5") }
            KeyButton("6", Modifier.weight(1f)) { append("6") }
            KeyButton("×", Modifier.weight(1f), tonal = true) { append("×") }
        }
        KeypadRow {
            KeyButton("1", Modifier.weight(1f)) { append("1") }
            KeyButton("2", Modifier.weight(1f)) { append("2") }
            KeyButton("3", Modifier.weight(1f)) { append("3") }
            KeyButton("−", Modifier.weight(1f), tonal = true) { append("−") }
        }
        KeypadRow {
            KeyButton("0", Modifier.weight(1f)) { append("0") }
            KeyButton(".", Modifier.weight(1f)) { append(".") }
            KeyButton("^", Modifier.weight(1f), tonal = true) { append("^") }
            KeyButton("+", Modifier.weight(1f), tonal = true) { append("+") }
        }

        Button(
            onClick = { evaluateNow() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = evaluation is EvalResult.Ok,
        ) { Text("=", style = MaterialTheme.typography.titleLarge) }

        if (history.isNotEmpty()) {
            SectionCard(title = "History") {
                history.forEachIndexed { index, entry ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            entry.expression,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            "= ${entry.result}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    if (index < history.lastIndex) HorizontalDivider()
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { history.clear() }) { Text("Clear history") }
            }
        }

        if (evaluation is EvalResult.Ok) {
            CopyableResult("Result", formatCalculatorResult(evaluation.value))
        }

        HintText(
            "Brackets may be implied, so 2(3+4) works. The ^ key raises to a power and " +
                "binds right to left, matching normal maths notation. For percentage " +
                "work — discounts, mark-ups, change — use the Percentages tool."
        )
    }
}

@Composable
private fun KeypadRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    onClick: () -> Unit,
) {
    val shared = modifier.height(56.dp)
    val text = @Composable { Text(label, style = MaterialTheme.typography.titleMedium) }
    if (tonal) {
        FilledTonalButton(onClick = onClick, modifier = shared, contentPadding = ButtonDefaults.TextButtonContentPadding) { text() }
    } else {
        OutlinedButton(onClick = onClick, modifier = shared, contentPadding = ButtonDefaults.TextButtonContentPadding) { text() }
    }
}
