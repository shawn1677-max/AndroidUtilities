package com.utilitybox.app.tools.convert

import android.Manifest
import android.content.Intent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.LocalSnackbar
import com.utilitybox.app.ui.common.PermissionGate
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.ui.common.rememberClipboardWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(onBack: () -> Unit) {
    ToolScaffold(title = "QR Scanner", onBack = onBack) {
        PermissionGate(
            permission = Manifest.permission.CAMERA,
            rationale = "Scanning a code needs the camera. Frames are decoded on the device " +
                "and discarded immediately — nothing is photographed, stored or sent " +
                "anywhere, and the app has no internet permission.",
        ) {
            ScannerContent()
        }
    }
}

@Composable
private fun ScannerContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val copyToClipboard = rememberClipboardWriter()
    val snackbar = LocalSnackbar.current

    var result by remember { mutableStateOf<ScanResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(executor) { onDispose { executor.shutdown() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        AndroidView(
            factory = { viewContext ->
                val previewView = PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                providerFuture.addListener({
                    runCatching {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(executor, BarcodeAnalyzer { scan -> result = scan }) }

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis,
                        )
                    }.onFailure { error = "Could not start the camera" }
                }, ContextCompat.getMainExecutor(viewContext))

                previewView
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    error?.let {
        SectionCard { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }

    val scan = result
    if (scan == null) {
        SectionCard {
            Text(
                "Point the camera at a QR code or barcode.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        SectionCard(title = "Result") {
            Text(scan.format, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(scan.text, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    copyToClipboard(scan.text)
                    snackbar("Copied")
                }) { Text("Copy") }

                if (scan.isOpenable) {
                    OutlinedButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, scan.text.toUri())
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }.onFailure { snackbar("No app can open this") }
                    }) { Text("Open") }
                }

                OutlinedButton(onClick = { result = null }) { Text("Scan again") }
            }
        }
    }

    HintText(
        "Reads QR, Data Matrix, Aztec, EAN, UPC, Code 128 and Code 39. Links are never " +
            "opened automatically — you always see the address first and choose whether to " +
            "follow it."
    )
}

private data class ScanResult(val text: String, val format: String) {
    /** Only well-known schemes get an Open button, so a scanned code cannot surprise you. */
    val isOpenable: Boolean
        get() = listOf("http://", "https://", "mailto:", "tel:", "sms:", "geo:")
            .any { text.startsWith(it, ignoreCase = true) }
}

private class BarcodeAnalyzer(private val onResult: (ScanResult) -> Unit) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.AZTEC,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.ITF,
                ),
                DecodeHintType.TRY_HARDER to true,
            )
        )
    }

    override fun analyze(image: ImageProxy) {
        try {
            // The Y plane alone is a luminance image, which is all ZXing needs.
            val plane = image.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val source = PlanarYUVLuminanceSource(
                data,
                plane.rowStride,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false,
            )
            val decoded = runCatching {
                reader.decode(BinaryBitmap(HybridBinarizer(source)))
            }.getOrNull()

            if (decoded != null) {
                onResult(ScanResult(decoded.text, decoded.barcodeFormat.name.replace('_', ' ')))
            }
        } catch (error: Exception) {
            // A frame that cannot be decoded is normal; simply wait for the next one.
        } finally {
            reader.reset()
            image.close()
        }
    }
}
