package com.utilitybox.app.tools.convert

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.CopyableResult
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.ui.common.rememberClipboardReader
import kotlinx.coroutines.delay

/** One "dit" in milliseconds at a comfortable practice speed. */
private const val DIT_MS = 140L

private val MORSE_TABLE = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
    'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
    'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
    'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
    'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
    'Z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
    '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
    '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
    '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...",
    ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
    '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-.",
)

private val REVERSE_TABLE = MORSE_TABLE.entries.associate { (key, value) -> value to key }

@Composable
fun MorseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var toMorse by remember { mutableStateOf(true) }
    var flashing by remember { mutableStateOf(false) }
    val pasteFromClipboard = rememberClipboardReader { input = it }

    val output = remember(input, toMorse) {
        if (toMorse) textToMorse(input) else morseToText(input)
    }

    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }
    val torchId = remember { cameraManager?.torchCameraId() }

    // Blinks the output as Morse on the camera flash.
    LaunchedEffect(flashing, output) {
        if (!flashing || torchId == null) return@LaunchedEffect
        val morse = if (toMorse) output else textToMorse(input)
        for (symbol in morse) {
            if (!flashing) break
            when (symbol) {
                '.' -> {
                    cameraManager?.setTorchSafely(torchId, true)
                    delay(DIT_MS)
                    cameraManager?.setTorchSafely(torchId, false)
                    delay(DIT_MS)
                }
                '-' -> {
                    cameraManager?.setTorchSafely(torchId, true)
                    delay(DIT_MS * 3)
                    cameraManager?.setTorchSafely(torchId, false)
                    delay(DIT_MS)
                }
                ' ' -> delay(DIT_MS * 2)
                '/' -> delay(DIT_MS * 4)
            }
        }
        cameraManager?.setTorchSafely(torchId, false)
        flashing = false
    }

    DisposableEffect(Unit) {
        onDispose { torchId?.let { cameraManager?.setTorchSafely(it, false) } }
    }

    ToolScaffold(title = "Morse Code", onBack = onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = toMorse,
                onClick = { toMorse = true },
                label = { Text("Text to Morse") },
            )
            FilterChip(
                selected = !toMorse,
                onClick = { toMorse = false },
                label = { Text("Morse to text") },
            )
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            label = { Text(if (toMorse) "Text" else "Morse (. and -)") },
            placeholder = {
                Text(if (toMorse) "Hello" else ".... . .-.. .-.. ---")
            },
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                fontFamily = if (toMorse) FontFamily.Default else FontFamily.Monospace,
            ),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = pasteFromClipboard) { Text("Paste") }
            TextButton(onClick = { input = "" }, enabled = input.isNotEmpty()) { Text("Clear") }
        }

        CopyableResult(
            label = if (toMorse) "Morse" else "Text",
            value = output,
            monospace = toMorse,
        )

        if (torchId != null) {
            Button(
                onClick = { flashing = !flashing },
                modifier = Modifier.fillMaxWidth(),
                enabled = input.isNotBlank(),
            ) { Text(if (flashing) "Stop flashing" else "Flash with the torch") }
        }

        SectionCard(title = "Reading Morse") {
            HintText(
                "A single space separates letters and a forward slash separates words, which " +
                    "is the standard written form. Timing follows the ITU standard: a dash is " +
                    "three dits long, gaps between letters are three dits and gaps between " +
                    "words are seven."
            )
        }
    }
}

internal fun textToMorse(text: String): String =
    text.trim().uppercase().split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" / ") { word ->
            word.mapNotNull { MORSE_TABLE[it] }.joinToString(" ")
        }

internal fun morseToText(morse: String): String =
    morse.trim().split(Regex("\\s*/\\s*"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.trim().split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .map { REVERSE_TABLE[it] ?: '?' }
                .joinToString("")
        }

private fun CameraManager.torchCameraId(): String? = runCatching {
    cameraIdList.firstOrNull { id ->
        getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }
}.getOrNull()

private fun CameraManager.setTorchSafely(id: String, enabled: Boolean) {
    runCatching { setTorchMode(id, enabled) }
}
