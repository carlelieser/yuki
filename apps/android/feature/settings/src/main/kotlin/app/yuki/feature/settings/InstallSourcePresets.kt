package app.yuki.feature.settings

import app.yuki.core.shizuku.SHELL_INSTALLER_PACKAGE

const val PLAY_STORE_PACKAGE = "com.android.vending"

data class InstallSourcePreset(val label: String, val packageName: String)

internal val INSTALL_SOURCE_PRESETS: List<InstallSourcePreset> = listOf(
    InstallSourcePreset(label = "Shell", packageName = SHELL_INSTALLER_PACKAGE),
    InstallSourcePreset(label = "Google Play Store", packageName = PLAY_STORE_PACKAGE),
)

internal fun installSourceApplies(mode: InstallMode): Boolean = mode == InstallMode.Shizuku

internal fun installSourceLabel(packageName: String, appLabel: String?): String =
    presetLabel(packageName) ?: appLabel ?: packageName

private fun presetLabel(packageName: String): String? = INSTALL_SOURCE_PRESETS
    .firstOrNull { preset -> preset.packageName == packageName }
    ?.label

internal const val INSTALL_SOURCE_TITLE = "Install source"
internal const val INSTALL_SOURCE_CHOOSE = "Choose app…"
internal const val INSTALL_SOURCE_DIALOG_TITLE = "Install source"
internal const val INSTALL_SOURCE_DISMISS = "Cancel"
const val INSTALL_SOURCE_SELECTOR_TAG = "installSourceSelector"
const val INSTALL_SOURCE_DIALOG_TAG = "installSourceDialog"
const val INSTALL_SOURCE_LIST_TAG = "installSourceList"
