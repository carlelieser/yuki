package app.yuki.feature.settings

import androidx.annotation.StringRes
import app.yuki.core.designsystem.component.StatusTone
import app.yuki.core.shizuku.ShizukuDetail
import app.yuki.core.shizuku.ShizukuMode
import app.yuki.core.shizuku.ShizukuState
import app.yuki.core.shizuku.UNKNOWN_API_VERSION

enum class ShizukuActionKind(@StringRes val label: Int) {
    OpenWebsite(R.string.settings_action_open_website),
    LaunchShizuku(R.string.settings_action_launch_shizuku),
    RequestPermission(R.string.settings_action_request_permission),
}

sealed interface ShizukuCardDescription {
    data class Fixed(@StringRes val text: Int) : ShizukuCardDescription

    data class Ready(
        val mode: ShizukuMode,
        val apiVersion: ShizukuApiVersion,
    ) : ShizukuCardDescription
}

data class ShizukuCard(
    @StringRes val title: Int,
    val description: ShizukuCardDescription,
    val tone: StatusTone,
    val actionKind: ShizukuActionKind?,
)

sealed interface ShizukuApiVersion {
    data object Unknown : ShizukuApiVersion

    data class Known(val level: Int) : ShizukuApiVersion
}

data class PermissionRow(
    val permission: AppPermission,
    val status: PermissionStatus,
) {
    val isGranted: Boolean get() = status == PermissionStatus.Granted

    @get:StringRes
    val statusLabel: Int
        get() = if (isGranted) {
            R.string.settings_permission_granted
        } else {
            R.string.settings_permission_denied
        }
}

data class InstallSourceChooser(val apps: List<InstalledApp>?)

data class InstallSourceSelection(
    val name: InstallSourceName,
    val isPlayStoreInstalled: Boolean,
    val isEnabled: Boolean,
)

data class SettingsContent(
    val shizuku: ShizukuDetail,
    val permissions: List<PermissionRow>,
    val preferences: YukiPreferences,
    val installSource: InstallSourceSelection,
) {
    val card: ShizukuCard get() = cardFor(shizuku)

    @get:StringRes
    val modeLabel: Int get() = modeLabelFor(shizuku.mode)

    val apiVersion: ShizukuApiVersion get() = apiVersionOf(shizuku.apiVersion)
}

private val fixedCards: Map<ShizukuState, ShizukuCard> = mapOf(
    ShizukuState.NotInstalled to ShizukuCard(
        title = R.string.settings_card_not_installed_title,
        description = ShizukuCardDescription.Fixed(
            R.string.settings_card_not_installed_description,
        ),
        tone = StatusTone.Informative,
        actionKind = ShizukuActionKind.OpenWebsite,
    ),
    ShizukuState.NotRunning to ShizukuCard(
        title = R.string.settings_card_not_running_title,
        description = ShizukuCardDescription.Fixed(
            R.string.settings_card_not_running_description,
        ),
        tone = StatusTone.Informative,
        actionKind = ShizukuActionKind.LaunchShizuku,
    ),
    ShizukuState.PermissionRequired to ShizukuCard(
        title = R.string.settings_card_permission_title,
        description = ShizukuCardDescription.Fixed(
            R.string.settings_card_permission_description,
        ),
        tone = StatusTone.Neutral,
        actionKind = ShizukuActionKind.RequestPermission,
    ),
)

internal fun cardFor(detail: ShizukuDetail): ShizukuCard =
    fixedCards[detail.state] ?: readyCard(detail)

private fun readyCard(detail: ShizukuDetail): ShizukuCard = ShizukuCard(
    title = R.string.settings_card_ready_title,
    description = ShizukuCardDescription.Ready(detail.mode, apiVersionOf(detail.apiVersion)),
    tone = StatusTone.Positive,
    actionKind = null,
)

private val modeLabels: Map<ShizukuMode, Int> = mapOf(
    ShizukuMode.AdbShell to R.string.settings_shizuku_mode_adb,
    ShizukuMode.Root to R.string.settings_shizuku_mode_root,
    ShizukuMode.Unknown to R.string.settings_shizuku_mode_unknown,
)

@StringRes
internal fun modeLabelFor(mode: ShizukuMode): Int = modeLabels.getValue(mode)

internal fun apiVersionOf(apiVersion: Int): ShizukuApiVersion = when (apiVersion) {
    UNKNOWN_API_VERSION -> ShizukuApiVersion.Unknown
    else -> ShizukuApiVersion.Known(apiVersion)
}
