package com.utilitybox.app.tools.device

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun StorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var volumes by remember { mutableStateOf<List<VolumeUsage>>(emptyList()) }
    var appUsage by remember { mutableStateOf<AppUsage?>(null) }

    LaunchedEffect(Unit) {
        volumes = withContext(Dispatchers.IO) { collectVolumes(context) }
        appUsage = withContext(Dispatchers.IO) { collectAppUsage(context) }
    }

    ToolScaffold(title = "Storage", onBack = onBack) {
        volumes.forEach { volume ->
            SectionCard(title = volume.name) {
                UsageBar(used = volume.used, total = volume.total)
                Spacer(Modifier.height(12.dp))
                InfoRow("Total", formatBytes(volume.total))
                InfoRow("Used", formatBytes(volume.used))
                InfoRow("Free", formatBytes(volume.free))
                InfoRow("Block size", formatBytes(volume.blockSize))
                InfoRow("Path", volume.path)
            }
        }

        appUsage?.let { usage ->
            SectionCard(title = "This app") {
                InfoRow("App data", formatBytes(usage.dataBytes))
                InfoRow("Cache", formatBytes(usage.cacheBytes))
            }
        }

        HintText(
            "Sizes come from the filesystem statistics Android exposes to every app. " +
                "A portion of every volume is reserved by the system and cannot be freed."
        )
    }
}

@Composable
private fun UsageBar(used: Long, total: Long) {
    val fraction = if (total > 0) (used.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
    )
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            String.format(Locale.US, "%.0f%% used", fraction * 100),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${formatBytes(total - used)} free",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class VolumeUsage(
    val name: String,
    val path: String,
    val total: Long,
    val free: Long,
    val blockSize: Long,
) {
    val used: Long get() = total - free
}

private data class AppUsage(val dataBytes: Long, val cacheBytes: Long)

private fun collectVolumes(context: Context): List<VolumeUsage> {
    val candidates = buildList {
        add("Internal storage" to Environment.getDataDirectory())
        add("System partition" to Environment.getRootDirectory())
        val shared = Environment.getExternalStorageDirectory()
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            add("Shared storage" to shared)
        }
        // Removable volumes the app can see without any storage permission.
        context.getExternalFilesDirs(null)
            .filterNotNull()
            .drop(1)
            .forEachIndexed { index, dir -> add("SD card ${index + 1}" to dir) }
    }

    return candidates.mapNotNull { (name, dir) ->
        runCatching {
            val stat = StatFs(dir.absolutePath)
            val blockSize = stat.blockSizeLong
            VolumeUsage(
                name = name,
                path = dir.absolutePath,
                total = stat.blockCountLong * blockSize,
                free = stat.availableBlocksLong * blockSize,
                blockSize = blockSize,
            )
        }.getOrNull()
    }.filter { it.total > 0 }.distinctBy { it.total to it.path }
}

private fun collectAppUsage(context: Context): AppUsage {
    val cache = directorySize(context.cacheDir) + directorySize(context.externalCacheDir)
    val data = directorySize(context.filesDir) + directorySize(context.appDataDir()) 
    return AppUsage(dataBytes = data, cacheBytes = cache)
}

private fun Context.appDataDir(): File = dataDir

private fun directorySize(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    if (dir.isFile) return dir.length()
    return dir.listFiles()?.sumOf { directorySize(it) } ?: 0L
}
