package app.yuki.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.SettingsItem
import app.yuki.core.designsystem.component.SettingsItemContent
import app.yuki.core.designsystem.component.YukiIcons

internal fun LazyListScope.preferencesSection(
    preferences: YukiPreferences,
    actions: PreferenceActions,
) {
    item { SectionHeader(title = PREFERENCES_SECTION_TITLE) }

    item {
        ToggleRow(
            content = ToggleContent(
                title = PRERELEASES_TITLE,
                supporting = PRERELEASES_SUPPORTING,
                isChecked = preferences.includePrereleases,
            ),
            onCheckedChange = actions.onIncludePrereleasesChange,
        )
    }

    item { InstallModeRow(mode = preferences.installMode, onChange = actions.onInstallModeChange) }

    item {
        ToggleRow(
            content = ToggleContent(
                title = DYNAMIC_COLOR_TITLE,
                supporting = DYNAMIC_COLOR_SUPPORTING,
                isChecked = preferences.isDynamicColorEnabled,
            ),
            onCheckedChange = actions.onDynamicColorChange,
        )
    }
}

internal data class ToggleContent(
    val title: String,
    val supporting: String,
    val isChecked: Boolean,
)

@Composable
private fun ToggleRow(content: ToggleContent, onCheckedChange: (Boolean) -> Unit) {
    SettingsItem(
        content = SettingsItemContent(
            title = content.title,
            supporting = content.supporting,
            trailing = {
                Switch(checked = content.isChecked, onCheckedChange = onCheckedChange)
            },
        ),
        onClick = { onCheckedChange(!content.isChecked) },
    )
}

@Composable
private fun InstallModeRow(mode: InstallMode, onChange: (InstallMode) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    SettingsItem(
        content = SettingsItemContent(
            title = INSTALL_MODE_TITLE,
            supporting = mode.label,
            trailing = {
                InstallModeSelector(
                    isExpanded = isExpanded,
                    onExpand = { isExpanded = true },
                    onDismiss = { isExpanded = false },
                    onSelect = { selected ->
                        isExpanded = false
                        onChange(selected)
                    },
                )
            },
        ),
        onClick = { isExpanded = true },
    )
}

@Composable
private fun InstallModeSelector(
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (InstallMode) -> Unit,
) {
    Box {
        IconButton(onClick = onExpand, modifier = Modifier.testTag(INSTALL_MODE_SELECTOR_TAG)) {
            Icon(
                imageVector = YukiIcons.ArrowDropDown,
                contentDescription = INSTALL_MODE_TITLE,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(expanded = isExpanded, onDismissRequest = onDismiss) {
            InstallMode.entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(text = entry.label) },
                    onClick = { onSelect(entry) },
                )
            }
        }
    }
}

internal const val PREFERENCES_SECTION_TITLE = "Preferences"
internal const val PRERELEASES_TITLE = "Include prereleases"
internal const val PRERELEASES_SUPPORTING = "Offer beta and release-candidate versions as updates."
internal const val INSTALL_MODE_TITLE = "Install mode"
const val INSTALL_MODE_SELECTOR_TAG = "installModeSelector"
internal const val DYNAMIC_COLOR_TITLE = "Dynamic color"
internal const val DYNAMIC_COLOR_SUPPORTING = "Match Yuki's palette to your wallpaper."
