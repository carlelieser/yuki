package app.yuki.feature.settings

import androidx.compose.foundation.layout.Box
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
import app.yuki.core.designsystem.component.SettingsGroup
import app.yuki.core.designsystem.component.SettingsRow
import app.yuki.core.designsystem.component.SettingsRowPosition
import app.yuki.core.designsystem.component.YukiIcons

private const val APPEARANCE_ROW_COUNT = 2
private const val PREFERENCE_ROW_COUNT = 3

@Composable
internal fun AppearanceSection(
    preferences: YukiPreferences,
    actions: PreferenceActions,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(modifier = modifier, label = APPEARANCE_SECTION_TITLE) {
        ThemeRow(
            position = SettingsRowPosition(index = 0, count = APPEARANCE_ROW_COUNT),
            mode = preferences.appearance,
            onChange = actions.onAppearanceChange,
        )

        ToggleRow(
            position = SettingsRowPosition(index = 1, count = APPEARANCE_ROW_COUNT),
            content = ToggleContent(
                title = DYNAMIC_COLOR_TITLE,
                supporting = DYNAMIC_COLOR_SUPPORTING,
                isChecked = preferences.isDynamicColorEnabled,
            ),
            onCheckedChange = actions.onDynamicColorChange,
        )
    }
}

@Composable
internal fun PreferencesSection(
    content: SettingsContent,
    actions: PreferenceActions,
    modifier: Modifier = Modifier,
) {
    val preferences = content.preferences

    SettingsGroup(modifier = modifier, label = PREFERENCES_SECTION_TITLE) {
        ToggleRow(
            position = SettingsRowPosition(index = 0, count = PREFERENCE_ROW_COUNT),
            content = ToggleContent(
                title = PRERELEASES_TITLE,
                supporting = PRERELEASES_SUPPORTING,
                isChecked = preferences.includePrereleases,
            ),
            onCheckedChange = actions.onIncludePrereleasesChange,
        )

        InstallModeRow(
            position = SettingsRowPosition(index = 1, count = PREFERENCE_ROW_COUNT),
            mode = preferences.installMode,
            onChange = actions.onInstallModeChange,
        )

        InstallSourceRow(
            position = SettingsRowPosition(index = 2, count = PREFERENCE_ROW_COUNT),
            source = content.installSource,
            actions = InstallSourceActions(
                onSelect = actions.onInstallerPackageChange,
                onChooseApp = actions.onChooseInstallerApp,
            ),
        )
    }
}

internal data class ToggleContent(
    val title: String,
    val supporting: String,
    val isChecked: Boolean,
)

@Composable
private fun ToggleRow(
    position: SettingsRowPosition,
    content: ToggleContent,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        position = position,
        title = content.title,
        supporting = content.supporting,
        onClick = { onCheckedChange(!content.isChecked) },
        trailing = { Switch(checked = content.isChecked, onCheckedChange = onCheckedChange) },
    )
}

@Composable
private fun ThemeRow(
    position: SettingsRowPosition,
    mode: AppearanceMode,
    onChange: (AppearanceMode) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    SettingsRow(
        position = position,
        title = THEME_TITLE,
        supporting = mode.label,
        onClick = { isExpanded = true },
        trailing = {
            ThemeSelector(
                isExpanded = isExpanded,
                onExpand = { isExpanded = true },
                onDismiss = { isExpanded = false },
                onSelect = { selected ->
                    isExpanded = false
                    onChange(selected)
                },
            )
        },
    )
}

@Composable
private fun ThemeSelector(
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (AppearanceMode) -> Unit,
) {
    Box {
        IconButton(onClick = onExpand, modifier = Modifier.testTag(APPEARANCE_SELECTOR_TAG)) {
            Icon(
                imageVector = YukiIcons.ArrowDropDown,
                contentDescription = THEME_TITLE,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(expanded = isExpanded, onDismissRequest = onDismiss) {
            AppearanceMode.entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(text = entry.label) },
                    onClick = { onSelect(entry) },
                )
            }
        }
    }
}

@Composable
private fun InstallModeRow(
    position: SettingsRowPosition,
    mode: InstallMode,
    onChange: (InstallMode) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    SettingsRow(
        position = position,
        title = INSTALL_MODE_TITLE,
        supporting = mode.label,
        onClick = { isExpanded = true },
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

internal const val APPEARANCE_SECTION_TITLE = "Appearance"
internal const val PREFERENCES_SECTION_TITLE = "Preferences"
internal const val PRERELEASES_TITLE = "Include prereleases"
internal const val PRERELEASES_SUPPORTING = "Offer beta and release-candidate versions as updates"
internal const val INSTALL_MODE_TITLE = "Install mode"
const val INSTALL_MODE_SELECTOR_TAG = "installModeSelector"
internal const val THEME_TITLE = "Theme"
const val APPEARANCE_SELECTOR_TAG = "appearanceSelector"
internal const val DYNAMIC_COLOR_TITLE = "Dynamic color"
internal const val DYNAMIC_COLOR_SUPPORTING = "Match Yuki's palette to your wallpaper"
