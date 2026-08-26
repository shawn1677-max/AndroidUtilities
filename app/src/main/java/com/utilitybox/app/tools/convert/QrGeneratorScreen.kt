package com.utilitybox.app.tools.convert

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.LocalSnackbar
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.io.File
import java.io.FileOutputStream

private enum class QrMode(val label: String) {
    TEXT("Text"),
    URL("Link"),
    WIFI("Wi-Fi"),
    CONTACT("Phone"),
}

@Composable
fun QrGeneratorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current

    var mode by remember { mutableStateOf(QrMode.TEXT) }
    var text by remember { mutableStateOf("") }
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var security by remember { mutableStateOf("WPA") }
    var errorCorrection by remember { mutableStateOf(ErrorCorrectionLevel.M) }

    val payload = when (mode) {
        QrMode.TEXT -> text
        QrMode.URL -> text.trim().let {
            if (it.isEmpty() || it.contains("://")) it else "https://$it"
        }
        QrMode.WIFI -> if (ssid.isBlank()) "" else buildWifiPayload(ssid, password, security)
        QrMode.CONTACT -> text.trim().let { if (it.isEmpty()) "" else "tel:$it" }
    }

    val bitmap = remember(payload, errorCorrection) {
        if (payload.isBlank()) null else encodeQr(payload, errorCorrection)
    }

    ToolScaffold(title = "QR Generator", onBack = onBack) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QrMode.entries.forEach { option ->
                FilterChip(
                    selected = mode == option,
                    onClick = { mode = option },
                    label = { Text(option.label) },
                )
            }
        }

        SectionCard {
            when (mode) {
                QrMode.WIFI -> {
                    OutlinedTextField(
                        value = ssid,
                        onValueChange = { ssid = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Network name (SSID)") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WPA", "WEP", "nopass").forEach { option ->
                            FilterChip(
                                selected = security == option,
                                onClick = { security = option },
                                label = { Text(if (option == "nopass") "Open" else option) },
                            )
                        }
                    }
                }

                else -> {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (mode == QrMode.TEXT) 120.dp else 64.dp),
                        label = {
                            Text(
                                when (mode) {
                                    QrMode.URL -> "Web address"
                                    QrMode.CONTACT -> "Phone number"
                                    else -> "Text"
                                }
                            )
                        },
                        singleLine = mode != QrMode.TEXT,
                    )
                }
            }
        }

        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Generated QR code",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                )
            }

            Button(
                onClick = {
                    val uri = saveForSharing(context, bitmap)
                    if (uri == null) {
                        snackbar("Could not prepare the image")
                    } else {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, "Share QR code"))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Share as image") }
        } else {
            SectionCard {
                Text(
                    "Enter something above to generate a code.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        SectionCard(title = "Error correction") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ErrorCorrectionLevel.L to "L 7%",
                    ErrorCorrectionLevel.M to "M 15%",
                    ErrorCorrectionLevel.Q to "Q 25%",
                    ErrorCorrectionLevel.H to "H 30%",
                ).forEach { (level, label) ->
                    FilterChip(
                        selected = errorCorrection == level,
                        onClick = { errorCorrection = level },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HintText(
                "Higher error correction survives smudges and printing on curved surfaces, " +
                    "at the cost of a denser code. M is a good default."
            )
        }

        HintText("Codes are generated on the device. Nothing is uploaded to make them.")
    }
}

/** Escapes the characters the Wi-Fi QR format treats as separators. */
private fun buildWifiPayload(ssid: String, password: String, security: String): String {
    fun escape(value: String) = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace(":", "\\:")
        .replace("\"", "\\\"")

    return if (security == "nopass") {
        "WIFI:T:nopass;S:${escape(ssid)};;"
    } else {
        "WIFI:T:$security;S:${escape(ssid)};P:${escape(password)};;"
    }
}

private fun encodeQr(content: String, level: ErrorCorrectionLevel, size: Int = 640): Bitmap? =
    runCatching {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to level,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 1,
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(matrix.width * matrix.height)
        for (y in 0 until matrix.height) {
            val offset = y * matrix.width
            for (x in 0 until matrix.width) {
                pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        createBitmap(matrix.width, matrix.height).apply {
            setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
        }
    }.getOrNull()

private fun saveForSharing(context: Context, bitmap: Bitmap): android.net.Uri? = runCatching {
    val directory = File(context.cacheDir, "qr").apply { mkdirs() }
    val file = File(directory, "qr-code.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}.getOrNull()
