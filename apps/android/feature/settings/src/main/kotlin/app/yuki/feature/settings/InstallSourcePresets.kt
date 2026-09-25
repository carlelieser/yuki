package app.yuki.feature.settings

import androidx.annotation.StringRes
import app.yuki.core.shizuku.SHELL_INSTALLER_PACKAGE

const val PLAY_STORE_PACKAGE = "com.android.vending"

data class InstallSourcePreset(@StringRes val label: Int, val packageName: String)

sealed interface InstallSourceName {
    data class Preset(@StringRes val label: Int) : InstallSourceName

    data class App(val label: String) : InstallSourceName

    data class Package(val packageName: String) : InstallSourceName
}

internal val INSTALL_SOURCE_PRESETS: List<InstallSourcePreset> = listOf(
    InstallSourcePreset(
        label = R.string.settings_install_source_shell,
        packageName = SHELL_INSTALLER_PACKAGE,
    ),
    InstallSourcePreset(
        label = R.string.settings_install_source_play_store,
        packageName = PLAY_STORE_PACKAGE,
    ),
)

internal fun installSourceApplies(mode: InstallMode): Boolean = mode == InstallMode.Shizuku

internal fun installSourceName(packageName: String, appLabel: String?): InstallSourceName {
    val preset = INSTALL_SOURCE_PRESETS.firstOrNull { preset -> preset.packageName == packageName }

    return when {
        preset != null -> InstallSourceName.Preset(preset.label)
        appLabel != null -> InstallSourceName.App(appLabel)
        else -> InstallSourceName.Package(packageName)
    }
}

const val INSTALL_SOURCE_SELECTOR_TAG = "installSourceSelector"
const val INSTALL_SOURCE_DIALOG_TAG = "installSourceDialog"
const val INSTALL_SOURCE_LIST_TAG = "installSourceList"
