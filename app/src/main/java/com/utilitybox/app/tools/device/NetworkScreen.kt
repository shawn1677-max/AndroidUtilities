package com.utilitybox.app.tools.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.util.Locale

@Composable
fun NetworkScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(NetworkSnapshot()) }

    LaunchedEffect(Unit) {
        while (true) {
            snapshot = withContext(Dispatchers.IO) { collectNetwork(context) }
            delay(3000)
        }
    }

    ToolScaffold(title = "Network Info", onBack = onBack) {
        SectionCard(title = "Active connection") {
            InfoRow("Type", snapshot.transport)
            InfoRow("Status", snapshot.status)
            InfoRow("Metered", snapshot.metered)
            InfoRow("VPN active", snapshot.vpn)
            if (snapshot.downstreamKbps != null) {
                InfoRow("Downstream estimate", formatKbps(snapshot.downstreamKbps!!))
            }
            if (snapshot.upstreamKbps != null) {
                InfoRow("Upstream estimate", formatKbps(snapshot.upstreamKbps!!))
            }
            InfoRow("Interface", snapshot.linkInterface)
            if (snapshot.dnsServers.isNotEmpty()) {
                InfoRow("DNS servers", snapshot.dnsServers.joinToString("\n"))
            }
        }

        SectionCard(title = "Carrier") {
            InfoRow("Operator", snapshot.operator)
            InfoRow("SIM country", snapshot.simCountry)
            InfoRow("Phone type", snapshot.phoneType)
        }

        if (snapshot.interfaces.isNotEmpty()) {
            SectionCard(title = "Interfaces") {
                snapshot.interfaces.forEach { iface ->
                    InfoRow(iface.name, iface.addresses.joinToString("\n"), monospace = true)
                }
            }
        }

        HintText(
            "Wi-Fi network names and signal strength are omitted on purpose: reading them " +
                "requires location access, which this app does not ask for."
        )
    }
}

private data class InterfaceInfo(val name: String, val addresses: List<String>)

private data class NetworkSnapshot(
    val transport: String = "—",
    val status: String = "—",
    val metered: String = "—",
    val vpn: String = "—",
    val downstreamKbps: Int? = null,
    val upstreamKbps: Int? = null,
    val linkInterface: String = "—",
    val dnsServers: List<String> = emptyList(),
    val operator: String = "—",
    val simCountry: String = "—",
    val phoneType: String = "—",
    val interfaces: List<InterfaceInfo> = emptyList(),
)

@Suppress("DEPRECATION") // PHONE_TYPE_CDMA is still the right constant for older SIMs.
private fun collectNetwork(context: Context): NetworkSnapshot {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val network = cm?.activeNetwork
    val caps = network?.let { cm.getNetworkCapabilities(it) }
    val link = network?.let { cm.getLinkProperties(it) }

    val transport = when {
        caps == null -> "Not connected"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        else -> "Other"
    }

    val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    return NetworkSnapshot(
        transport = transport,
        status = when {
            caps == null -> "Offline"
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> "Connected and validated"
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "Connected, not validated"
            else -> "Connected, no internet capability"
        },
        metered = when {
            caps == null -> "—"
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> "No"
            else -> "Yes"
        },
        vpn = when {
            caps == null -> "—"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "Yes"
            else -> "No"
        },
        downstreamKbps = caps?.linkDownstreamBandwidthKbps?.takeIf { it > 0 },
        upstreamKbps = caps?.linkUpstreamBandwidthKbps?.takeIf { it > 0 },
        linkInterface = link?.interfaceName ?: "—",
        dnsServers = link?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList(),
        operator = telephony?.networkOperatorName?.takeIf { it.isNotBlank() } ?: "—",
        simCountry = telephony?.simCountryIso?.uppercase(Locale.US)?.takeIf { it.isNotBlank() } ?: "—",
        phoneType = when (telephony?.phoneType) {
            TelephonyManager.PHONE_TYPE_GSM -> "GSM"
            TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
            TelephonyManager.PHONE_TYPE_SIP -> "SIP"
            TelephonyManager.PHONE_TYPE_NONE -> "No telephony"
            else -> "—"
        },
        interfaces = collectInterfaces(),
    )
}

private fun collectInterfaces(): List<InterfaceInfo> = runCatching {
    NetworkInterface.getNetworkInterfaces()
        ?.toList()
        .orEmpty()
        .filter { it.isUp && !it.isLoopback }
        .mapNotNull { iface ->
            val addresses = iface.inetAddresses.toList().mapNotNull { address ->
                when (address) {
                    is Inet4Address -> address.hostAddress
                    is Inet6Address -> address.hostAddress?.substringBefore('%')
                    else -> null
                }
            }
            if (addresses.isEmpty()) null else InterfaceInfo(iface.name, addresses)
        }
}.getOrDefault(emptyList())

private fun formatKbps(kbps: Int): String = when {
    kbps >= 1000 -> String.format(Locale.US, "%.1f Mbps", kbps / 1000.0)
    else -> "$kbps Kbps"
}
