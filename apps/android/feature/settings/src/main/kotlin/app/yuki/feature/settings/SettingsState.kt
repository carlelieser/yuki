package app.yuki.feature.settings

import app.yuki.core.designsystem.component.StatusTone
import app.yuki.core.shizuku.ShizukuDetail
import app.yuki.core.shizuku.ShizukuMode
import app.yuki.core.shizuku.ShizukuState
import app.yuki.core.shizuku.UNKNOWN_API_VERSION

enum class ShizukuActionKind {
    OpenWebsite,
    LaunchShizuku,
    RequestPermission,
}

data class ShizukuCard(
    val title: String,
    val description: String,
    val tone: StatusTone,
    val actionLabel: String?,
    val actionKind: ShizukuActionKind?,
)

data class PermissionRow(
    val permission: AppPermission,
    val status: PermissionStatus,
) {
    val isGranted: Boolean get() = status == PermissionStatus.Granted

    val statusLabel: String get() = if (isGranted) STATUS_GRANTED else STATUS_DENIED
}

data class InstallSourceChooser(val apps: List<InstalledApp>?)

data class InstallSourceSelection(
    val label: String,
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

    val modeLabel: String get() = modeLabelFor(shizuku)

    val apiVersionLabel: String get() = apiVersionLabelFor(shizuku.apiVersion)
}

internal fun cardFor(detail: ShizukuDetail): ShizukuCard = when (detail.state) {
    ShizukuState.NotInstalled -> ShizukuCard(
        title = CARD_NOT_INSTALLED_TITLE,
        description = CARD_NOT_INSTALLED_DESCRIPTION,
        tone = StatusTone.Informative,
        actionLabel = ACTION_OPEN_WEBSITE,
        actionKind = ShizukuActionKind.OpenWebsite,
    )

    ShizukuState.NotRunning -> ShizukuCard(
        title = CARD_NOT_RUNNING_TITLE,
        description = CARD_NOT_RUNNING_DESCRIPTION,
        tone = StatusTone.Informative,
        actionLabel = ACTION_LAUNCH_SHIZUKU,
        actionKind = ShizukuActionKind.LaunchShizuku,
    )

    ShizukuState.PermissionRequired -> ShizukuCard(
        title = CARD_PERMISSION_TITLE,
        description = CARD_PERMISSION_DESCRIPTION,
        tone = StatusTone.Neutral,
        actionLabel = ACTION_REQUEST_PERMISSION,
        actionKind = ShizukuActionKind.RequestPermission,
    )

    ShizukuState.Ready -> ShizukuCard(
        title = CARD_READY_TITLE,
        description = readyDescription(detail),
        tone = StatusTone.Positive,
        actionLabel = null,
        actionKind = null,
    )
}

private fun readyDescription(detail: ShizukuDetail): String =
    "$CARD_READY_DESCRIPTION ${modeLabelFor(detail)}, ${apiVersionLabelFor(detail.apiVersion)}"

internal fun modeLabelFor(detail: ShizukuDetail): String = when (detail.mode) {
    ShizukuMode.AdbShell -> MODE_ADB
    ShizukuMode.Root -> MODE_ROOT
    ShizukuMode.Unknown -> MODE_UNKNOWN
}

internal fun apiVersionLabelFor(apiVersion: Int): String =
    if (apiVersion == UNKNOWN_API_VERSION) API_UNKNOWN else "API $apiVersion"

internal const val STATUS_GRANTED = "Granted"
internal const val STATUS_DENIED = "Denied"

internal const val CARD_NOT_INSTALLED_TITLE = "Silent install unavailable"
internal const val CARD_NOT_INSTALLED_DESCRIPTION =
    "Shizuku is not installed. Without it, every install needs a system prompt"
internal const val CARD_NOT_RUNNING_TITLE = "Shizuku installed, not started"
internal const val CARD_NOT_RUNNING_DESCRIPTION =
    "Start the Shizuku service to let Yuki install apps without a prompt"
internal const val CARD_PERMISSION_TITLE = "Awaiting authorization"
internal const val CARD_PERMISSION_DESCRIPTION =
    "Shizuku is running. Authorize Yuki to use it for silent installs"
internal const val CARD_READY_TITLE = "Silent install active"
internal const val CARD_READY_DESCRIPTION = "Yuki installs without a prompt through"

internal const val ACTION_OPEN_WEBSITE = "Get Shizuku"
internal const val ACTION_LAUNCH_SHIZUKU = "Launch Shizuku"
internal const val ACTION_REQUEST_PERMISSION = "Request permission"

internal const val MODE_ADB = "ADB shell"
internal const val MODE_ROOT = "Root"
internal const val MODE_UNKNOWN = "Not resolved"
internal const val API_UNKNOWN = "Version unknown"
