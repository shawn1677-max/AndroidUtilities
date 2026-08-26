package com.utilitybox.app.tools.calculate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.BigReadout
import com.utilitybox.app.ui.common.CopyableResult
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.security.SecureRandom

private enum class RandomMode(val label: String) {
    NUMBER("Number"),
    DICE("Dice"),
    COIN("Coin"),
    LIST("Pick from a list"),
}

@Composable
fun RandomScreen(onBack: () -> Unit) {
    val random = remember { SecureRandom() }
    var mode by remember { mutableStateOf(RandomMode.NUMBER) }

    ToolScaffold(title = "Random Picker", onBack = onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RandomMode.entries.forEach { option ->
                FilterChip(
                    selected = mode == option,
                    onClick = { mode = option },
                    label = { Text(option.label) },
                )
            }
        }

        when (mode) {
            RandomMode.NUMBER -> NumberPicker(random)
            RandomMode.DICE -> DicePicker(random)
            RandomMode.COIN -> CoinPicker(random)
            RandomMode.LIST -> ListPicker(random)
        }

        HintText(
            "Every draw uses the device's cryptographic random source, so results are not " +
                "predictable from previous ones."
        )
    }
}

@Composable
private fun NumberPicker(random: SecureRandom) {
    var minimum by remember { mutableStateOf("1") }
    var maximum by remember { mutableStateOf("100") }
    var count by remember { mutableIntStateOf(1) }
    var unique by remember { mutableStateOf(true) }
    var results by remember { mutableStateOf<List<Int>>(emptyList()) }

    val low = minimum.toIntOrNull()
    val high = maximum.toIntOrNull()
    val valid = low != null && high != null && high >= low

    SectionCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = minimum,
                onValueChange = { minimum = it.filter { c -> c.isDigit() || c == '-' } },
                modifier = Modifier.weight(1f),
                label = { Text("From") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = maximum,
                onValueChange = { maximum = it.filter { c -> c.isDigit() || c == '-' } },
                modifier = Modifier.weight(1f),
                label = { Text("To") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("How many: $count", style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Slider(
            value = count.toFloat(),
            onValueChange = { count = it.toInt().coerceIn(1, 20) },
            valueRange = 1f..20f,
            steps = 18,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("No repeats", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = unique, onCheckedChange = { unique = it })
        }
    }

    Button(
        onClick = {
            if (!valid) return@Button
            val range = low!!..high!!
            val size = high - low + 1
            results = if (unique && count <= size) {
                range.shuffled(java.util.Random(random.nextLong())).take(count)
            } else {
                (1..count).map { low + random.nextInt(size) }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = valid,
    ) { Text("Draw") }

    if (results.isNotEmpty()) {
        if (results.size == 1) {
            SectionCard { BigReadout(results.first().toString()) }
        } else {
            CopyableResult("Results", results.joinToString(", "))
        }
    }
}

@Composable
private fun DicePicker(random: SecureRandom) {
    var sides by remember { mutableIntStateOf(6) }
    var dice by remember { mutableIntStateOf(2) }
    var rolls by remember { mutableStateOf<List<Int>>(emptyList()) }

    SectionCard {
        Text("Sides per die: $sides", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(4, 6, 8, 10, 12, 20).forEach { value ->
                FilterChip(
                    selected = sides == value,
                    onClick = { sides = value },
                    label = { Text("d$value") },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Number of dice: $dice", style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Slider(
            value = dice.toFloat(),
            onValueChange = { dice = it.toInt().coerceIn(1, 10) },
            valueRange = 1f..10f,
            steps = 8,
        )
    }

    Button(
        onClick = { rolls = (1..dice).map { random.nextInt(sides) + 1 } },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Roll ${dice}d$sides") }

    if (rolls.isNotEmpty()) {
        SectionCard {
            BigReadout(rolls.sum().toString())
            Spacer(Modifier.height(8.dp))
            Text(
                rolls.joinToString("  +  "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CoinPicker(random: SecureRandom) {
    var flips by remember { mutableStateOf<List<Boolean>>(emptyList()) }
    var count by remember { mutableIntStateOf(1) }

    SectionCard {
        Text("Coins: $count", style = MaterialTheme.typography.bodyMedium)
        androidx.compose.material3.Slider(
            value = count.toFloat(),
            onValueChange = { count = it.toInt().coerceIn(1, 20) },
            valueRange = 1f..20f,
            steps = 18,
        )
    }

    Button(
        onClick = { flips = (1..count).map { random.nextBoolean() } },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Flip") }

    if (flips.isNotEmpty()) {
        SectionCard {
            if (flips.size == 1) {
                BigReadout(if (flips.first()) "Heads" else "Tails")
            } else {
                Text(
                    flips.joinToString(" ") { if (it) "H" else "T" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                InfoRow("Heads", flips.count { it }.toString(), copyable = false)
                InfoRow("Tails", flips.count { !it }.toString(), copyable = false)
            }
        }
    }
}

@Composable
private fun ListPicker(random: SecureRandom) {
    var text by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<String?>(null) }
    var shuffled by remember { mutableStateOf<List<String>>(emptyList()) }

    val items = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        label = { Text("One option per line") },
        placeholder = { Text("Alice\nBob\nCharlie") },
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = {
                picked = items.getOrNull(random.nextInt(items.size.coerceAtLeast(1)))
                shuffled = emptyList()
            },
            modifier = Modifier.weight(1f),
            enabled = items.isNotEmpty(),
        ) { Text("Pick one") }

        Button(
            onClick = {
                shuffled = items.shuffled(java.util.Random(random.nextLong()))
                picked = null
            },
            modifier = Modifier.weight(1f),
            enabled = items.size > 1,
        ) { Text("Shuffle") }
    }

    picked?.let { SectionCard { BigReadout(it) } }

    if (shuffled.isNotEmpty()) {
        CopyableResult(
            "Shuffled order",
            shuffled.mapIndexed { index, item -> "${index + 1}. $item" }.joinToString("\n"),
            monospace = false,
        )
    }

    if (items.isNotEmpty()) {
        HintText("${items.size} options entered.")
    }
}
