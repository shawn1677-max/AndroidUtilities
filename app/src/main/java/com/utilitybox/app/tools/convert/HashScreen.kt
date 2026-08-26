package com.utilitybox.app.tools.convert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import java.security.MessageDigest
import java.util.Locale

private val ALGORITHMS = listOf("MD5", "SHA-1", "SHA-256", "SHA-384", "SHA-512")

@Composable
fun HashScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var uppercase by remember { mutableStateOf(false) }
    val pasteFromClipboard = rememberClipboardReader { input = it }

    ToolScaffold(title = "Hash Generator", onBack = onBack) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            label = { Text("Text to hash") },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = pasteFromClipboard) { Text("Paste") }
                TextButton(onClick = { input = "" }, enabled = input.isNotEmpty()) {
                    Text("Clear")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Uppercase")
                Switch(checked = uppercase, onCheckedChange = { uppercase = it })
            }
        }

        ALGORITHMS.forEach { algorithm ->
            val digest = remember(input, algorithm, uppercase) {
                hash(input, algorithm).let { if (uppercase) it.uppercase(Locale.US) else it }
            }
            CopyableResult(algorithm, digest)
        }

        SectionCard(title = "Which one should I use?") {
            HintText(
                "SHA-256 is the sensible default for verifying downloads and file integrity. " +
                    "MD5 and SHA-1 are here because older tools still publish them, but both " +
                    "are broken for security purposes — never use them to protect anything. " +
                    "None of these are suitable for storing passwords; that needs a slow, " +
                    "salted algorithm such as bcrypt or Argon2."
            )
        }
    }
}

private fun hash(input: String, algorithm: String): String {
    if (input.isEmpty()) return ""
    return runCatching {
        MessageDigest.getInstance(algorithm)
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }.getOrDefault("Unavailable on this device")
}
