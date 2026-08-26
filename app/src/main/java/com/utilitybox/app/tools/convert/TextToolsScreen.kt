package com.utilitybox.app.tools.convert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.LocalSnackbar
import com.utilitybox.app.ui.common.rememberClipboardReader
import com.utilitybox.app.ui.common.rememberClipboardWriter
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextToolsScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val copyToClipboard = rememberClipboardWriter()
    val pasteFromClipboard = rememberClipboardReader { text = it }
    val snackbar = LocalSnackbar.current

    val stats = remember(text) { TextStats.of(text) }

    val transforms: List<Pair<String, (String) -> String>> = listOf(
        "UPPER CASE" to { it.uppercase(Locale.getDefault()) },
        "lower case" to { it.lowercase(Locale.getDefault()) },
        "Title Case" to ::toTitleCase,
        "Sentence case" to ::toSentenceCase,
        "camelCase" to ::toCamelCase,
        "snake_case" to { toDelimited(it, "_") },
        "kebab-case" to { toDelimited(it, "-") },
        "Reverse" to { it.reversed() },
        "Trim spaces" to { it.trim().replace(Regex("[ \\t]+"), " ") },
        "Remove blank lines" to { input -> input.lines().filter { it.isNotBlank() }.joinToString("\n") },
        "Sort lines" to { input -> input.lines().sorted().joinToString("\n") },
        "Unique lines" to { input -> input.lines().distinct().joinToString("\n") },
    )

    ToolScaffold(title = "Text Tools", onBack = onBack) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            label = { Text("Text") },
            placeholder = { Text("Paste or type here") },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = pasteFromClipboard) { Text("Paste") }
            TextButton(
                onClick = {
                    copyToClipboard(text)
                    snackbar("Copied")
                },
                enabled = text.isNotEmpty(),
            ) { Text("Copy") }
            TextButton(onClick = { text = "" }, enabled = text.isNotEmpty()) { Text("Clear") }
        }

        SectionCard(title = "Transform") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                transforms.forEach { (label, transform) ->
                    AssistChip(
                        onClick = { text = transform(text) },
                        label = { Text(label) },
                    )
                }
            }
        }

        SectionCard(title = "Statistics") {
            InfoRow("Characters", stats.characters.toString())
            InfoRow("Characters (no spaces)", stats.charactersNoSpaces.toString())
            InfoRow("Words", stats.words.toString())
            InfoRow("Sentences", stats.sentences.toString())
            InfoRow("Lines", stats.lines.toString())
            InfoRow("Paragraphs", stats.paragraphs.toString())
            InfoRow("Longest word", stats.longestWord)
            InfoRow("Reading time", stats.readingTime)
        }

        HintText(
            "Transforms apply to the whole text box. Reading time assumes 200 words per minute."
        )
    }
}

private data class TextStats(
    val characters: Int,
    val charactersNoSpaces: Int,
    val words: Int,
    val sentences: Int,
    val lines: Int,
    val paragraphs: Int,
    val longestWord: String,
    val readingTime: String,
) {
    companion object {
        fun of(text: String): TextStats {
            val wordList = text.split(Regex("\\s+")).filter { it.isNotBlank() }
            val minutes = wordList.size / 200.0
            return TextStats(
                characters = text.length,
                charactersNoSpaces = text.count { !it.isWhitespace() },
                words = wordList.size,
                sentences = text.split(Regex("[.!?]+")).count { it.isNotBlank() },
                lines = if (text.isEmpty()) 0 else text.lines().size,
                paragraphs = text.split(Regex("\n\\s*\n")).count { it.isNotBlank() },
                longestWord = wordList.maxByOrNull { it.length } ?: "—",
                readingTime = when {
                    wordList.isEmpty() -> "—"
                    minutes < 1 -> "under a minute"
                    else -> "${Math.round(minutes)} min"
                },
            )
        }
    }
}

private fun toTitleCase(text: String): String =
    text.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

private fun toSentenceCase(text: String): String {
    val lower = text.lowercase(Locale.getDefault())
    val builder = StringBuilder(lower)
    var capitaliseNext = true
    for (index in builder.indices) {
        val character = builder[index]
        if (capitaliseNext && character.isLetter()) {
            builder[index] = character.uppercaseChar()
            capitaliseNext = false
        } else if (character in ".!?") {
            capitaliseNext = true
        }
    }
    return builder.toString()
}

private fun words(text: String): List<String> =
    text.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }

private fun toCamelCase(text: String): String =
    words(text).mapIndexed { index, word ->
        if (index == 0) word.lowercase(Locale.getDefault())
        else word.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercaseChar() }
    }.joinToString("")

private fun toDelimited(text: String, delimiter: String): String =
    words(text).joinToString(delimiter) { it.lowercase(Locale.getDefault()) }
