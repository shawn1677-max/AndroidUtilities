package com.utilitybox.app.tools.device

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.utilitybox.app.ui.common.LocalSnackbar
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.util.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun AppInventoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
        loading = false
    }

    val filtered = remember(apps, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) apps
        else apps.filter { it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
    }

    ToolScaffold(title = "App Inventory", onBack = onBack, scrollable = false) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Filter by name or package") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                )
            }
            item {
                Text(
                    text = when {
                        loading -> "Reading installed apps…"
                        else -> "${filtered.size} of ${apps.size} launchable apps"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(filtered, key = { it.packageName }) { app -> AppCard(app) }
        }
    }
}

@Composable
private fun AppCard(app: AppEntry) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(app.label, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                "${app.packageName} · v${app.versionName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                DetailLine("Version code", app.versionCode.toString())
                DetailLine("APK size", formatBytes(app.apkSize))
                DetailLine("Target SDK", app.targetSdk.toString())
                DetailLine("Min SDK", app.minSdk.toString())
                DetailLine("Installed", app.firstInstall)
                DetailLine("Updated", app.lastUpdate)
                DetailLine("System app", if (app.isSystem) "Yes" else "No")

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            val intent = context.packageManager
                                .getLaunchIntentForPackage(app.packageName)
                            if (intent != null) {
                                context.startActivity(intent)
                            } else {
                                snackbar("This app cannot be launched")
                            }
                        },
                        label = { Text("Open") },
                    )
                    AssistChip(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", app.packageName, null),
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }.onFailure { snackbar("Could not open app settings") }
                        },
                        label = { Text("App info") },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private data class AppEntry(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val apkSize: Long,
    val targetSdk: Int,
    val minSdk: Int,
    val firstInstall: String,
    val lastUpdate: String,
    val isSystem: Boolean,
)

/**
 * Uses the manifest <queries> launcher filter rather than the restricted
 * QUERY_ALL_PACKAGES permission, so only apps the user can actually open appear.
 */
@Suppress("DEPRECATION")
private fun loadLaunchableApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        pm.queryIntentActivities(intent, 0)
    }
    val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

    return resolved
        .map { it.activityInfo.packageName }
        .distinct()
        .mapNotNull { packageName ->
            runCatching {
                val info = pm.getPackageInfo(packageName, 0)
                val appInfo: ApplicationInfo = info.applicationInfo ?: return@runCatching null
                AppEntry(
                    label = pm.getApplicationLabel(appInfo).toString(),
                    packageName = packageName,
                    versionName = info.versionName ?: "—",
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        info.longVersionCode
                    } else {
                        info.versionCode.toLong()
                    },
                    apkSize = runCatching { File(appInfo.sourceDir).length() }.getOrDefault(0L),
                    targetSdk = appInfo.targetSdkVersion,
                    minSdk = appInfo.minSdkVersion,
                    firstInstall = dateFormat.format(Date(info.firstInstallTime)),
                    lastUpdate = dateFormat.format(Date(info.lastUpdateTime)),
                    isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }.getOrNull()
        }
        .sortedBy { it.label.lowercase() }
}
