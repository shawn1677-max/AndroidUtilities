package com.utilitybox.app.tools.convert

import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.CopyableResult
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.ui.common.rememberClipboardReader

@Composable
fun Base64Screen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var encoding by remember { mutableStateOf(true) }
    var urlSafe by remember { mutableStateOf(false) }
    var wrapLines by remember { mutableStateOf(false) }
    val pasteFromClipboard = rememberClipboardReader { input = it }

    val output = remember(input, encoding, urlSafe, wrapLines) {
        convert(input, encoding, urlSafe, wrapLines)
    }

    ToolScaffold(title = "Base64", onBack = onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = encoding,
                onClick = { encoding = true },
                label = { Text("Encode") },
            )
            FilterChip(
                selected = !encoding,
                onClick = { encoding = false },
                label = { Text("Decode") },
            )
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            label = { Text(if (encoding) "Plain text" else "Base64") },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = pasteFromClipboard) { Text("Paste") }
            TextButton(onClick = { input = "" }, enabled = input.isNotEmpty()) { Text("Clear") }
            TextButton(onClick = {
                // Round-tripping the result makes it easy to check your own work.
                input = output.takeIf { !it.startsWith("Not valid") } ?: input
                encoding = !encoding
            }) { Text("Swap") }
        }

        CopyableResult(
            label = if (encoding) "Base64" else "Plain text",
            value = output,
            monospace = encoding,
        )

        SectionCard(title = "Options") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("URL-safe alphabet", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = urlSafe, onCheckedChange = { urlSafe = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Wrap at 76 characters", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = wrapLines, onCheckedChange = { wrapLines = it })
            }
            Spacer(Modifier.height(8.dp))
            HintText(
                "The URL-safe alphabet replaces + and / with - and _, which is what JSON Web " +
                    "Tokens and query strings use. Decoding accepts either alphabet."
            )
        }
    }
}

private fun convert(input: String, encoding: Boolean, urlSafe: Boolean, wrapLines: Boolean): String {
    if (input.isEmpty()) return ""
    return if (encoding) {
        var flags = if (wrapLines) 0 else Base64.NO_WRAP
        if (urlSafe) flags = flags or Base64.URL_SAFE
        runCatching {
            Base64.encodeToString(input.toByteArray(Charsets.UTF_8), flags).trimEnd('\n')
        }.getOrDefault("Could not encode")
    } else {
        runCatching {
            // Normalise the URL-safe alphabet back to the standard one so either form decodes.
            val cleaned = input.trim()
                .replace("\n", "")
                .replace(" ", "")
                .replace('-', '+')
                .replace('_', '/')
            String(Base64.decode(cleaned, Base64.DEFAULT), Charsets.UTF_8)
        }.getOrDefault("Not valid Base64")
    }
}
