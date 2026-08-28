package com.utilitybox.app.tools.convert

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

private const val PREFS = "utilitybox_world_clock"
private const val KEY_ZONES = "zones"

@Composable
fun WorldClockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val zones = remember { mutableStateListOf<String>().apply { addAll(loadZones(context)) } }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    var shiftHours by remember { mutableFloatStateOf(0f) }
    var showPicker by remember { mutableStateOf(false) }

    // A minute is enough: nothing here displays seconds.
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1_000)
        }
    }

    val reference = now.plusMinutes((shiftHours * 60).roundToInt().toLong())
    val timeFormat = remember {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
    }
    val dayFormat = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()) }

    ToolScaffold(title = "World Clock", onBack = onBack) {
        SectionCard(title = "Here") {
            Text(
                reference.format(timeFormat),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                "${reference.format(dayFormat)} · ${ZoneId.systemDefault().id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    shiftHours == 0f -> "Showing now"
                    shiftHours > 0 -> "Showing ${formatShift(shiftHours)} from now"
                    else -> "Showing ${formatShift(-shiftHours)} ago"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = shiftHours,
                onValueChange = { shiftHours = it },
                valueRange = -24f..24f,
                steps = 95,
            )
            if (shiftHours != 0f) {
                TextButton(onClick = { shiftHours = 0f }) { Text("Back to now") }
            }
        }

        zones.forEachIndexed { index, zoneId ->
            val zone = runCatching { ZoneId.of(zoneId) }.getOrNull()
            if (zone != null) {
                val there = reference.withZoneSameInstant(zone)
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(zoneLabel(zoneId), style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${there.format(dayFormat)} · ${offsetLabel(reference, there)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            there.format(timeFormat),
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace,
                        )
                        IconButton(onClick = {
                            zones.removeAt(index)
                            saveZones(context, zones.toList())
                        }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Remove ${zoneLabel(zoneId)}",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        Button(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add a city")
        }

        HintText(
            "Offsets come from the time zone database on the device, so daylight saving " +
                "is handled for you — including the cases where a city's offset changes " +
                "between now and the time you are previewing."
        )
    }

    if (showPicker) {
        ZonePickerDialog(
            existing = zones.toSet(),
            onDismiss = { showPicker = false },
            onPick = { picked ->
                zones.add(picked)
                saveZones(context, zones.toList())
                showPicker = false
            },
        )
    }
}

@Composable
private fun ZonePickerDialog(
    existing: Set<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val all = remember { ZoneId.getAvailableZoneIds().sorted() }
    val matches = remember(query, existing) {
        val q = query.trim().lowercase()
        all.asSequence()
            .filter { it !in existing }
            .filter { q.isEmpty() || it.lowercase().contains(q) }
            .take(40)
            .toList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a city") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search, e.g. Tokyo") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(matches.size) { index ->
                        val zoneId = matches[index]
                        TextButton(
                            onClick = { onPick(zoneId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                zoneLabel(zoneId),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/** "Europe/London" reads better as "London — Europe". */
internal fun zoneLabel(zoneId: String): String {
    val parts = zoneId.split('/')
    val city = parts.last().replace('_', ' ')
    return if (parts.size > 1) "$city — ${parts.first().replace('_', ' ')}" else city
}

/** Difference from the reference zone, as people say it: "+9h", "−3h30". */
internal fun offsetLabel(reference: ZonedDateTime, there: ZonedDateTime): String {
    val minutes = Duration.between(
        reference.toLocalDateTime(),
        there.toLocalDateTime(),
    ).toMinutes()
    if (minutes == 0L) return "same time"
    val sign = if (minutes > 0) "+" else "−"
    val absolute = kotlin.math.abs(minutes)
    val hours = absolute / 60
    val remainder = absolute % 60
    return if (remainder == 0L) "$sign${hours}h" else "$sign${hours}h${remainder}"
}

private fun formatShift(hours: Float): String {
    val totalMinutes = (hours * 60).roundToInt()
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}

private fun loadZones(context: Context): List<String> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_ZONES, null)
        ?.split('\n')
        ?.filter { it.isNotBlank() }
        ?: listOf("America/New_York", "Europe/London", "Asia/Tokyo")

private fun saveZones(context: Context, zones: List<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit { putString(KEY_ZONES, zones.joinToString("\n")) }
}
