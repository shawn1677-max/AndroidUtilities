package com.utilitybox.app.tools.measure

import android.Manifest
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.PermissionGate
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.util.Locale

@Composable
fun MagnifierScreen(onBack: () -> Unit) {
    ToolScaffold(title = "Magnifier", onBack = onBack) {
        PermissionGate(
            permission = Manifest.permission.CAMERA,
            rationale = "The magnifier shows a zoomed live view through the camera, so it " +
                "needs camera access. Nothing is photographed, stored or sent anywhere — " +
                "the app has no internet permission.",
        ) {
            MagnifierContent()
        }
    }
}

@Composable
private fun MagnifierContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoom by remember { mutableFloatStateOf(0f) }
    var torchOn by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Linear zoom is 0..1 across whatever range the lens actually supports,
    // so the slider means the same thing on every device.
    DisposableEffect(camera, zoom) {
        runCatching { camera?.cameraControl?.setLinearZoom(zoom.coerceIn(0f, 1f)) }
        onDispose { }
    }
    DisposableEffect(camera, torchOn) {
        runCatching { camera?.cameraControl?.enableTorch(torchOn) }
        onDispose { }
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { camera?.cameraControl?.enableTorch(false) } }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
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
                        provider.unbindAll()
                        camera = provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
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

    SectionCard(title = "Zoom") {
        Text(
            String.format(Locale.US, "%.0f%% of this lens's range", zoom * 100),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(value = zoom, onValueChange = { zoom = it })
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { torchOn = !torchOn },
                modifier = Modifier.weight(1f),
            ) { Text(if (torchOn) "Light off" else "Light on") }

            OutlinedButton(
                onClick = { zoom = 0f },
                modifier = Modifier.weight(1f),
            ) { Text("Reset zoom") }
        }
    }

    HintText(
        "Hold the phone steady and a few inches back — most phone cameras cannot focus " +
            "closer than that, so backing off and zooming in reads better than moving " +
            "nearer. The light helps on printed text but reflects badly off glossy paper."
    )
}
