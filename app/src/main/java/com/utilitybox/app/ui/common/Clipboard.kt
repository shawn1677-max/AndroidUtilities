package com.utilitybox.app.ui.common

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

/**
 * Clipboard access lives behind these two helpers so the suspending platform API
 * is wrapped once instead of in every tool that copies or pastes.
 */

/** Returns a function that puts [text] on the clipboard under a friendly label. */
@Composable
fun rememberClipboardWriter(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope) {
        { text: String ->
            scope.launch {
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Utility Box", text)))
            }
            Unit
        }
    }
}

/**
 * Returns a function that reads the clipboard and hands any plain text to
 * [onText]. Nothing happens when the clipboard is empty or holds another type.
 */
@Composable
fun rememberClipboardReader(onText: (String) -> Unit): () -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope, onText) {
        {
            scope.launch {
                val entry = clipboard.getClipEntry()
                val clip = entry?.clipData
                if (clip != null && clip.itemCount > 0) {
                    clip.getItemAt(0).text?.toString()?.let(onText)
                }
            }
            Unit
        }
    }
}
