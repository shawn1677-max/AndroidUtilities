package com.utilitybox.app.tools.convert

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.ui.common.rememberClipboardReader
import java.util.Locale

@Composable
fun TextReaderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var speed by remember { mutableFloatStateOf(1f) }
    var pitch by remember { mutableFloatStateOf(1f) }
    var ready by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    val pasteFromClipboard = rememberClipboardReader { text = it }

    DisposableEffect(Unit) {
        var created: TextToSpeech? = null
        created = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = created?.setLanguage(Locale.getDefault())
                failure = when (result) {
                    TextToSpeech.LANG_MISSING_DATA ->
                        "Voice data for your language is not installed. " +
                            "Android's text-to-speech settings can download it."
                    TextToSpeech.LANG_NOT_SUPPORTED ->
                        "Your language is not supported by the installed voice."
                    else -> null
                }
                ready = true
            } else {
                failure = "No text-to-speech engine is available on this device."
            }
        }
        engine = created
        onDispose {
            created.stop()
            created.shutdown()
            engine = null
        }
    }

    ToolScaffold(title = "Text Reader", onBack = onBack) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            label = { Text("Text to read aloud") },
            placeholder = { Text("Paste or type here") },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = pasteFromClipboard) { Text("Paste") }
            TextButton(onClick = { text = "" }, enabled = text.isNotEmpty()) { Text("Clear") }
        }

        failure?.let { message ->
            SectionCard {
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    engine?.apply {
                        setSpeechRate(speed)
                        setPitch(pitch)
                        speak(text, TextToSpeech.QUEUE_FLUSH, null, "utilitybox")
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = ready && text.isNotBlank(),
            ) { Text("Read aloud") }

            OutlinedButton(
                onClick = { engine?.stop() },
                modifier = Modifier.weight(1f),
                enabled = ready,
            ) { Text("Stop") }
        }

        SectionCard(title = "Voice") {
            Text("Speed ${String.format(Locale.US, "%.1fx", speed)}",
                style = MaterialTheme.typography.bodyMedium)
            Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2.5f)
            Spacer(Modifier.height(8.dp))
            Text("Pitch ${String.format(Locale.US, "%.1f", pitch)}",
                style = MaterialTheme.typography.bodyMedium)
            Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..2f)
        }

        HintText(
            "Uses the text-to-speech engine already on your device, so it works offline " +
                "and nothing is sent anywhere. Changing speed or pitch takes effect the " +
                "next time you press Read aloud."
        )
    }
}
