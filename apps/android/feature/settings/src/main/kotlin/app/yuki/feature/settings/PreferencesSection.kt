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
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.SettingsGroup
import app.yuki.core.designsystem.component.SettingsRow
import app.yuki.core.designsystem.component.SettingsRowPosition
import app.yuki.core.designsystem.component.YukiIcons

private const val APPEARANCE_ROW_COUNT = 2
private const val PREFERENCE_ROW_COUNT = 4

@Composable
internal fun AppearanceSection(
    preferences: YukiPreferences,
    actions: PreferenceActions,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(modifier = modifier, label = stringResource(R.string.settings_appearance_title)) {
        ThemeRow(
            position = SettingsRowPosition(index = 0, count = APPEARANCE_ROW_COUNT),
            mode = preferences.appearance,
            onChange = actions.onAppearanceChange,
        )

        ToggleRow(
            position = SettingsRowPosition(index = 1, count = APPEARANCE_ROW_COUNT),
            content = ToggleContent(
                title = stringResource(R.string.settings_dynamic_color_title),
                supporting = stringResource(R.string.settings_dynamic_color_supporting),
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

    SettingsGroup(
        modifier = modifier,
        label = stringResource(R.string.settings_preferences_title),
    ) {
        UpdateToggleRows(preferences = preferences, actions = actions)

        InstallModeRow(
            position = SettingsRowPosition(index = 2, count = PREFERENCE_ROW_COUNT),
            mode = preferences.installMode,
            onChange = actions.onInstallModeChange,
        )

        InstallSourceRow(
            position = SettingsRowPosition(index = 3, count = PREFERENCE_ROW_COUNT),
            source = content.installSource,
            actions = InstallSourceActions(
                onSelect = actions.onInstallerPackageChange,
                onChooseApp = actions.onChooseInstallerApp,
            ),
        )
    }
}

@Composable
private fun UpdateToggleRows(preferences: YukiPreferences, actions: PreferenceActions) {
    ToggleRow(
        position = SettingsRowPosition(index = 0, count = PREFERENCE_ROW_COUNT),
        content = ToggleContent(
            title = stringResource(R.string.settings_auto_update_check_title),
            supporting = stringResource(R.string.settings_auto_update_check_supporting),
            isChecked = preferences.isAutoUpdateCheckEnabled,
        ),
        onCheckedChange = actions.onAutoUpdateCheckChange,
    )

    ToggleRow(
        position = SettingsRowPosition(index = 1, count = PREFERENCE_ROW_COUNT),
        content = ToggleContent(
            title = stringResource(R.string.settings_prereleases_title),
            supporting = stringResource(R.string.settings_prereleases_supporting),
            isChecked = preferences.includePrereleases,
        ),
        onCheckedChange = actions.onIncludePrereleasesChange,
    )
}

internal data class ToggleContent(
    val title: String,
    val supporting: String,
    val isChecked: Boolean,
)

@Composable
internal fun ToggleRow(
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
        title = stringResource(R.string.settings_theme_title),
        supporting = stringResource(mode.label),
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
                contentDescription = stringResource(R.string.settings_theme_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(expanded = isExpanded, onDismissRequest = onDismiss) {
            AppearanceMode.entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(entry.label)) },
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
        title = stringResource(R.string.settings_install_mode_title),
        supporting = stringResource(mode.label),
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
                contentDescription = stringResource(R.string.settings_install_mode_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(expanded = isExpanded, onDismissRequest = onDismiss) {
            InstallMode.entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(entry.label)) },
                    onClick = { onSelect(entry) },
                )
            }
        }
    }
}

const val INSTALL_MODE_SELECTOR_TAG = "installModeSelector"
const val APPEARANCE_SELECTOR_TAG = "appearanceSelector"
