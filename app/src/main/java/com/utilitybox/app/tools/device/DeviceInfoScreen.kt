package com.utilitybox.app.tools.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.formatBytes
import com.utilitybox.app.util.formatUptime
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun DeviceInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val info = remember { collectDeviceInfo(context) }
    var uptime by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var availableRam by remember { mutableLongStateOf(readAvailableRam(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            uptime = SystemClock.elapsedRealtime()
            availableRam = readAvailableRam(context)
        }
    }

    ToolScaffold(title = "Device Info", onBack = onBack) {
        SectionCard(title = "Device") {
            InfoRow("Manufacturer", Build.MANUFACTURER)
            InfoRow("Brand", Build.BRAND)
            InfoRow("Model", Build.MODEL)
            InfoRow("Device", Build.DEVICE)
            InfoRow("Product", Build.PRODUCT)
            InfoRow("Board", Build.BOARD)
            InfoRow("Hardware", Build.HARDWARE)
        }

        SectionCard(title = "Android") {
            InfoRow("Version", Build.VERSION.RELEASE)
            InfoRow("API level", Build.VERSION.SDK_INT.toString())
            InfoRow("Codename", Build.VERSION.CODENAME)
            InfoRow("Security patch", Build.VERSION.SECURITY_PATCH)
            InfoRow("Build ID", Build.ID)
            InfoRow("Build type", Build.TYPE)
            InfoRow("Bootloader", Build.BOOTLOADER)
            InfoRow("Fingerprint", Build.FINGERPRINT)
            InfoRow("Uptime", formatUptime(uptime))
        }

        SectionCard(title = "Processor") {
            InfoRow("SoC manufacturer", info.socManufacturer)
            InfoRow("SoC model", info.socModel)
            InfoRow("CPU cores", info.cores.toString())
            InfoRow("Max clock", info.maxClock)
            InfoRow("Supported ABIs", info.abis)
            InfoRow("Instruction set", info.primaryAbi)
        }

        SectionCard(title = "Memory") {
            InfoRow("Total RAM", formatBytes(info.totalRam))
            InfoRow("Available RAM", formatBytes(availableRam))
            InfoRow("Used RAM", formatBytes(info.totalRam - availableRam))
            InfoRow("Low memory threshold", formatBytes(info.lowMemoryThreshold))
            InfoRow("Heap limit per app", "${info.heapLimitMb} MB")
        }

        SectionCard(title = "Display") {
            InfoRow("Resolution", "${info.widthPx} x ${info.heightPx} px")
            InfoRow("Density", "${info.densityDpi} dpi (${info.densityBucket})")
            InfoRow("Scale factor", String.format(Locale.US, "%.2fx", info.density))
            InfoRow("Logical size", "${info.widthDp} x ${info.heightDp} dp")
            InfoRow("Refresh rate", String.format(Locale.US, "%.1f Hz", info.refreshRate))
            InfoRow("Diagonal", String.format(Locale.US, "%.2f in", info.diagonalInches))
            InfoRow("Physical DPI", "${info.xdpi.roundToInt()} x ${info.ydpi.roundToInt()}")
        }

        HintText(
            "All values are read from the Android platform APIs on this device. " +
                "Nothing is uploaded — this app has no internet permission."
        )
    }
}

private data class DeviceInfo(
    val socManufacturer: String,
    val socModel: String,
    val cores: Int,
    val maxClock: String,
    val abis: String,
    val primaryAbi: String,
    val totalRam: Long,
    val lowMemoryThreshold: Long,
    val heapLimitMb: Int,
    val widthPx: Int,
    val heightPx: Int,
    val widthDp: Int,
    val heightDp: Int,
    val densityDpi: Int,
    val density: Float,
    val densityBucket: String,
    val refreshRate: Float,
    val diagonalInches: Double,
    val xdpi: Float,
    val ydpi: Float,
)

@Suppress("DEPRECATION")
private fun collectDeviceInfo(context: Context): DeviceInfo {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    val widthPx: Int
    val heightPx: Int
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.maximumWindowMetrics.bounds
        widthPx = bounds.width()
        heightPx = bounds.height()
        // densityDpi/xdpi still come from resources, which track the same display.
        metrics.setTo(context.resources.displayMetrics)
    } else {
        windowManager.defaultDisplay.getRealMetrics(metrics)
        widthPx = metrics.widthPixels
        heightPx = metrics.heightPixels
    }

    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        windowManager.defaultDisplay
    }

    val xdpi = if (metrics.xdpi > 0f) metrics.xdpi else metrics.densityDpi.toFloat()
    val ydpi = if (metrics.ydpi > 0f) metrics.ydpi else metrics.densityDpi.toFloat()
    val widthInches = widthPx / xdpi
    val heightInches = heightPx / ydpi
    val diagonal = sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())

    return DeviceInfo(
        socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER
        } else {
            readCpuInfoField("Hardware") ?: "Not reported"
        },
        socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL
        } else {
            readCpuInfoField("model name") ?: "Not reported"
        },
        cores = Runtime.getRuntime().availableProcessors(),
        maxClock = readMaxClock(),
        abis = Build.SUPPORTED_ABIS.joinToString(", "),
        primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
        totalRam = memoryInfo.totalMem,
        lowMemoryThreshold = memoryInfo.threshold,
        heapLimitMb = activityManager.memoryClass,
        widthPx = widthPx,
        heightPx = heightPx,
        widthDp = (widthPx / metrics.density).roundToInt(),
        heightDp = (heightPx / metrics.density).roundToInt(),
        densityDpi = metrics.densityDpi,
        density = metrics.density,
        densityBucket = densityBucket(metrics.densityDpi),
        refreshRate = display?.refreshRate ?: 0f,
        diagonalInches = diagonal,
        xdpi = xdpi,
        ydpi = ydpi,
    )
}

private fun readAvailableRam(context: Context): Long {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }.availMem
}

private fun densityBucket(dpi: Int): String = when {
    dpi <= 120 -> "ldpi"
    dpi <= 160 -> "mdpi"
    dpi <= 240 -> "hdpi"
    dpi <= 320 -> "xhdpi"
    dpi <= 480 -> "xxhdpi"
    dpi <= 640 -> "xxxhdpi"
    else -> "${dpi}dpi"
}

/** /proc/cpuinfo is world readable and needs no permission. */
private fun readCpuInfoField(field: String): String? = runCatching {
    File("/proc/cpuinfo").useLines { lines ->
        lines.firstOrNull { it.startsWith(field, ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }
}.getOrNull()

private fun readMaxClock(): String {
    val khz = (0 until Runtime.getRuntime().availableProcessors()).mapNotNull { core ->
        runCatching {
            File("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_max_freq")
                .readText().trim().toLongOrNull()
        }.getOrNull()
    }.maxOrNull() ?: return "Not reported"
    return String.format(Locale.US, "%.2f GHz", khz / 1_000_000.0)
}
