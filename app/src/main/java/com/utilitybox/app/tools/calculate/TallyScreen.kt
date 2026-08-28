package com.utilitybox.app.tools.calculate

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold

private const val PREFS = "utilitybox_tally"
private const val KEY_COUNTERS = "counters"

/** name and value, stored as one line each so the whole set is a single string. */
data class Counter(val name: String, val value: Int)

@Composable
fun TallyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val counters = remember { mutableStateListOf<Counter>().apply { addAll(loadCounters(context)) } }
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    fun persist() = saveCounters(context, counters.toList())

    ToolScaffold(title = "Tally Counter", onBack = onBack) {
        if (counters.isEmpty()) {
            SectionCard {
                Text(
                    "No counters yet. Add one to start counting.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        counters.forEachIndexed { index, counter ->
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        counter.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        counters.removeAt(index)
                        persist()
                    }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete ${counter.name}",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Text(
                    text = counter.value.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            counters[index] = counter.copy(value = counter.value - 1)
                            persist()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                    ) { Text("−1", style = MaterialTheme.typography.titleMedium) }

                    Button(
                        onClick = {
                            counters[index] = counter.copy(value = counter.value + 1)
                            persist()
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp),
                    ) { Text("+1", style = MaterialTheme.typography.titleLarge) }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = {
                    counters[index] = counter.copy(value = 0)
                    persist()
                }) { Text("Reset to zero") }
            }
        }

        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add a counter")
        }

        HintText(
            "Counters are saved on the device as you tap, so closing the app — or the " +
                "app being killed in the background — keeps your count."
        )
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false; newName = "" },
            title = { Text("New counter") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.take(30) },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newName.trim().ifEmpty { "Counter ${counters.size + 1}" }
                        counters.add(Counter(name, 0))
                        saveCounters(context, counters.toList())
                        newName = ""
                        showAdd = false
                    },
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false; newName = "" }) { Text("Cancel") }
            },
        )
    }
}

/**
 * Stored as "value\tname" per line. Tab and newline are stripped from names on
 * the way in, so a name can never corrupt the record separator.
 */
internal fun encodeCounters(counters: List<Counter>): String =
    counters.joinToString("\n") { counter ->
        val safeName = counter.name.replace('\t', ' ').replace('\n', ' ')
        "${counter.value}\t$safeName"
    }

internal fun decodeCounters(raw: String): List<Counter> =
    raw.lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val separator = line.indexOf('\t')
            if (separator <= 0) return@mapNotNull null
            val value = line.substring(0, separator).toIntOrNull() ?: return@mapNotNull null
            Counter(name = line.substring(separator + 1), value = value)
        }
        .toList()

private fun loadCounters(context: Context): List<Counter> =
    decodeCounters(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_COUNTERS, "") ?: ""
    )

private fun saveCounters(context: Context, counters: List<Counter>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit { putString(KEY_COUNTERS, encodeCounters(counters)) }
}
