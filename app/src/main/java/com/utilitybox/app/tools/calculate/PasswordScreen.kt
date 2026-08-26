package com.utilitybox.app.tools.calculate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.CopyableResult
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.security.SecureRandom
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

private const val LOWER = "abcdefghijklmnopqrstuvwxyz"
private const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val DIGITS = "0123456789"
private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?/"
private const val AMBIGUOUS = "Il1O0o"

@Composable
fun PasswordScreen(onBack: () -> Unit) {
    val random = remember { SecureRandom() }

    var passphraseMode by remember { mutableStateOf(false) }
    var length by remember { mutableIntStateOf(20) }
    var wordCount by remember { mutableIntStateOf(5) }
    var useUpper by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var avoidAmbiguous by remember { mutableStateOf(true) }
    var separator by remember { mutableStateOf("-") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(
        passphraseMode, length, wordCount, useUpper, useDigits,
        useSymbols, avoidAmbiguous, separator, refreshKey,
    ) {
        password = if (passphraseMode) {
            generatePassphrase(random, wordCount, separator, useDigits)
        } else {
            generatePassword(random, length, useUpper, useDigits, useSymbols, avoidAmbiguous)
        }
    }

    val entropy = if (passphraseMode) {
        wordCount * PassphraseWords.BITS_PER_WORD + if (useDigits) 10.0 else 0.0
    } else {
        val alphabet = alphabetSize(useUpper, useDigits, useSymbols, avoidAmbiguous)
        if (alphabet > 1) length * (ln(alphabet.toDouble()) / ln(2.0)) else 0.0
    }

    ToolScaffold(title = "Password Generator", onBack = onBack) {
        CopyableResult("Generated", password)

        Button(onClick = { refreshKey++ }, modifier = Modifier.fillMaxWidth()) {
            Text("Generate another")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !passphraseMode,
                onClick = { passphraseMode = false },
                label = { Text("Password") },
            )
            FilterChip(
                selected = passphraseMode,
                onClick = { passphraseMode = true },
                label = { Text("Passphrase") },
            )
        }

        SectionCard(title = "Strength") {
            LinearProgressIndicator(
                progress = { (entropy / 128.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            Spacer(Modifier.height(10.dp))
            InfoRow("Entropy", String.format(Locale.US, "%.0f bits", entropy), copyable = false)
            InfoRow("Rating", strengthLabel(entropy), copyable = false)
            InfoRow("Offline guessing", crackTime(entropy), copyable = false)
        }

        if (passphraseMode) {
            SectionCard(title = "Passphrase options") {
                Text("Words: $wordCount", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = wordCount.toFloat(),
                    onValueChange = { wordCount = it.toInt().coerceIn(3, 10) },
                    valueRange = 3f..10f,
                    steps = 6,
                )
                Spacer(Modifier.height(8.dp))
                Text("Separator", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("-" to "dash", "." to "dot", "_" to "underscore", " " to "space").forEach { (value, label) ->
                        FilterChip(
                            selected = separator == value,
                            onClick = { separator = value },
                            label = { Text(label) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                ToggleRow("Append a number", useDigits) { useDigits = it }
            }
        } else {
            SectionCard(title = "Password options") {
                Text("Length: $length", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = length.toFloat(),
                    onValueChange = { length = it.toInt().coerceIn(6, 64) },
                    valueRange = 6f..64f,
                )
                ToggleRow("Uppercase letters", useUpper) { useUpper = it }
                ToggleRow("Digits", useDigits) { useDigits = it }
                ToggleRow("Symbols", useSymbols) { useSymbols = it }
                ToggleRow("Avoid look-alike characters", avoidAmbiguous) { avoidAmbiguous = it }
            }
        }

        HintText(
            "Characters come from the system's cryptographic random source, and nothing is " +
                "stored or transmitted — close the screen and the password is gone. A " +
                "passphrase of five or more words is easy to type and remember while still " +
                "being very hard to guess. For anything important, let a password manager " +
                "store it rather than relying on memory."
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun alphabet(
    useUpper: Boolean,
    useDigits: Boolean,
    useSymbols: Boolean,
    avoidAmbiguous: Boolean,
): String {
    val builder = StringBuilder(LOWER)
    if (useUpper) builder.append(UPPER)
    if (useDigits) builder.append(DIGITS)
    if (useSymbols) builder.append(SYMBOLS)
    val pool = builder.toString()
    return if (avoidAmbiguous) pool.filter { it !in AMBIGUOUS } else pool
}

private fun alphabetSize(
    useUpper: Boolean,
    useDigits: Boolean,
    useSymbols: Boolean,
    avoidAmbiguous: Boolean,
): Int = alphabet(useUpper, useDigits, useSymbols, avoidAmbiguous).length

private fun generatePassword(
    random: SecureRandom,
    length: Int,
    useUpper: Boolean,
    useDigits: Boolean,
    useSymbols: Boolean,
    avoidAmbiguous: Boolean,
): String {
    val pool = alphabet(useUpper, useDigits, useSymbols, avoidAmbiguous)
    if (pool.isEmpty()) return ""
    // Rejection-free: nextInt(bound) is already uniform for a SecureRandom.
    return (1..length).map { pool[random.nextInt(pool.length)] }.joinToString("")
}

private fun generatePassphrase(
    random: SecureRandom,
    wordCount: Int,
    separator: String,
    appendNumber: Boolean,
): String {
    val words = PassphraseWords.words
    val phrase = (1..wordCount)
        .map { words[random.nextInt(words.size)] }
        .joinToString(separator)
    return if (appendNumber) "$phrase$separator${random.nextInt(1000).toString().padStart(3, '0')}" else phrase
}

private fun strengthLabel(entropy: Double): String = when {
    entropy < 40 -> "Weak"
    entropy < 60 -> "Reasonable"
    entropy < 80 -> "Strong"
    entropy < 110 -> "Very strong"
    else -> "Excellent"
}

/** Assumes 100 billion guesses per second, a realistic offline attack on a fast rig. */
private fun crackTime(entropy: Double): String {
    if (entropy <= 0) return "—"
    val guesses = 2.0.pow(entropy - 1)
    val seconds = guesses / 1e11
    return when {
        seconds < 60 -> "seconds"
        seconds < 3600 -> "${(seconds / 60).toInt()} minutes"
        seconds < 86_400 -> "${(seconds / 3600).toInt()} hours"
        seconds < 31_536_000 -> "${(seconds / 86_400).toInt()} days"
        seconds < 31_536_000e3 -> "${(seconds / 31_536_000).toInt()} years"
        seconds < 31_536_000e9 -> String.format(Locale.US, "%.0f thousand years", seconds / 31_536_000e3)
        else -> "longer than the age of the universe"
    }
}
