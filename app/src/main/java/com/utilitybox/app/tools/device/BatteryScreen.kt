package com.utilitybox.app.tools.device

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.utilitybox.app.ui.common.BigReadout
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import java.util.Locale

@Composable
fun BatteryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BatteryState()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent != null) state = readBattery(context, intent)
            }
        }
        // ACTION_BATTERY_CHANGED is sticky, so registering immediately yields the
        // current value and then every subsequent change.
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (sticky != null) state = readBattery(context, sticky)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    ToolScaffold(title = "Battery", onBack = onBack) {
        SectionCard {
            BigReadout(value = state.percent?.toString() ?: "—", unit = "%")
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (state.percent ?: 0) / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(title = "Charging") {
            InfoRow("Status", state.status)
            InfoRow("Power source", state.plugged)
            InfoRow("Health", state.health)
            if (state.chargeTimeRemaining != null) {
                InfoRow("Time to full", state.chargeTimeRemaining!!)
            }
        }

        SectionCard(title = "Electrical") {
            InfoRow("Voltage", state.voltage)
            InfoRow("Temperature", state.temperature)
            InfoRow("Technology", state.technology)
            InfoRow("Current now", state.currentNow)
            InfoRow("Average current", state.currentAverage)
            InfoRow("Charge counter", state.chargeCounter)
            InfoRow("Energy counter", state.energyCounter)
        }

        HintText(
            "Current readings come from the battery fuel gauge. A negative current means " +
                "the battery is discharging. Some manufacturers do not populate every field."
        )
    }
}

private data class BatteryState(
    val percent: Int? = null,
    val status: String = "Reading…",
    val plugged: String = "—",
    val health: String = "—",
    val voltage: String = "—",
    val temperature: String = "—",
    val technology: String = "—",
    val currentNow: String = "—",
    val currentAverage: String = "—",
    val chargeCounter: String = "—",
    val energyCounter: String = "—",
    val chargeTimeRemaining: String? = null,
)

private fun readBattery(context: Context, intent: Intent): BatteryState {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val percent = if (level >= 0 && scale > 0) level * 100 / scale else null

    val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
    val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)

    return BatteryState(
        percent = percent,
        status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        },
        plugged = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            0 -> "Battery"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Unknown"
        },
        health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
            else -> "Unknown"
        },
        voltage = if (voltageMv > 0) String.format(Locale.US, "%.3f V", voltageMv / 1000.0) else "—",
        temperature = if (tempTenths != Int.MIN_VALUE) {
            val celsius = tempTenths / 10.0
            String.format(Locale.US, "%.1f °C  (%.1f °F)", celsius, celsius * 9 / 5 + 32)
        } else {
            "—"
        },
        technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—",
        currentNow = manager.microAmpsOrDash(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
        currentAverage = manager.microAmpsOrDash(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE),
        chargeCounter = manager
            ?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.takeIf { it > 0 }
            ?.let { String.format(Locale.US, "%,d mAh", it / 1000) }
            ?: "—",
        energyCounter = manager
            ?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            ?.takeIf { it > 0 && it != Long.MIN_VALUE }
            ?.let { String.format(Locale.US, "%.2f Wh", it / 1_000_000_000.0) }
            ?: "—",
        chargeTimeRemaining = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager?.computeChargeTimeRemaining()
                ?.takeIf { it > 0 }
                ?.let { millis ->
                    val minutes = millis / 60_000
                    String.format(Locale.US, "%d h %02d min", minutes / 60, minutes % 60)
                }
        } else {
            null
        },
    )
}

/** Fuel gauges report microamps; some devices report 0 or MIN_VALUE when unsupported. */
private fun BatteryManager?.microAmpsOrDash(property: Int): String {
    val raw = this?.getIntProperty(property) ?: return "—"
    if (raw == 0 || raw == Int.MIN_VALUE) return "—"
    return String.format(Locale.US, "%,d mA", raw / 1000)
}
