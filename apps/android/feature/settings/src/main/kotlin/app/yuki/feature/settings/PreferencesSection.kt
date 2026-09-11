package app.yuki.feature.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.SettingsItem
import app.yuki.core.designsystem.component.SettingsItemContent

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
    SettingsItem(
        content = SettingsItemContent(
            title = INSTALL_MODE_TITLE,
            supporting = installModeSupporting(mode),
            trailing = {
                Switch(
                    checked = mode == InstallMode.Automatic,
                    onCheckedChange = { isAutomatic -> onChange(installModeFor(isAutomatic)) },
                )
            },
        ),
        onClick = { onChange(nextMode(mode)) },
    )
}

internal fun installModeFor(isAutomatic: Boolean): InstallMode =
    if (isAutomatic) InstallMode.Automatic else InstallMode.AlwaysAsk

internal fun nextMode(mode: InstallMode): InstallMode =
    installModeFor(mode != InstallMode.Automatic)

internal fun installModeSupporting(mode: InstallMode): String = when (mode) {
    InstallMode.Automatic -> INSTALL_MODE_AUTOMATIC
    InstallMode.AlwaysAsk -> INSTALL_MODE_ALWAYS_ASK
}

internal const val PREFERENCES_SECTION_TITLE = "Preferences"
internal const val PRERELEASES_TITLE = "Include prereleases"
internal const val PRERELEASES_SUPPORTING = "Offer beta and release-candidate versions as updates."
internal const val INSTALL_MODE_TITLE = "Install mode"
internal const val INSTALL_MODE_AUTOMATIC = "Install silently when Shizuku is ready."
internal const val INSTALL_MODE_ALWAYS_ASK = "Confirm before every install."
internal const val DYNAMIC_COLOR_TITLE = "Dynamic color"
internal const val DYNAMIC_COLOR_SUPPORTING = "Match Yuki's palette to your wallpaper."
