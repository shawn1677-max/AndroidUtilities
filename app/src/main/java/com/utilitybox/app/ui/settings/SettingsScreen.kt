package com.utilitybox.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.utilitybox.app.BuildConfig
import com.utilitybox.app.ui.common.HintText
import com.utilitybox.app.ui.common.InfoRow
import com.utilitybox.app.ui.common.SectionCard
import com.utilitybox.app.ui.common.ToolScaffold
import com.utilitybox.app.ui.theme.LocalThemeController
import com.utilitybox.app.ui.theme.ThemeMode

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val themeController = LocalThemeController.current

    ToolScaffold(title = "Settings", onBack = onBack) {
        SectionCard(title = "Appearance") {
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { option ->
                    ThemeOptionRow(
                        option = option,
                        selected = themeController.mode == option,
                        onSelect = { themeController.mode = option },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HintText(
                "The choice applies straight away and is remembered. On Android 12 and " +
                    "later the accent colours still follow your wallpaper in both themes."
            )
        }

        SectionCard(title = "About") {
            InfoRow("Version", BuildConfig.VERSION_NAME)
            InfoRow("Package", BuildConfig.APPLICATION_ID)
        }
    }
}

@Composable
private fun ThemeOptionRow(
    option: ThemeMode,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(option.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                option.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
