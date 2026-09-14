package app.yuki.feature.settings

data class PreferenceActions(
    val onIncludePrereleasesChange: (Boolean) -> Unit,
    val onInstallModeChange: (InstallMode) -> Unit,
    val onAppearanceChange: (AppearanceMode) -> Unit,
    val onDynamicColorChange: (Boolean) -> Unit,
)

data class SettingsActions(
    val onShizukuAction: (ShizukuActionKind) -> Unit,
    val onPermissionClick: (PermissionRow) -> Unit,
    val preferences: PreferenceActions,
)

internal fun shizukuActionHandler(
    destinations: SystemDestinations,
    onRequestPermission: () -> Unit,
): (ShizukuActionKind) -> Unit = { kind ->
    when (kind) {
        ShizukuActionKind.OpenWebsite -> destinations.openShizukuWebsite()
        ShizukuActionKind.LaunchShizuku -> destinations.launchShizuku()
        ShizukuActionKind.RequestPermission -> onRequestPermission()
    }
}

internal fun permissionClickHandler(
    destinations: SystemDestinations,
    onRequestPermission: () -> Unit,
): (PermissionRow) -> Unit = { row ->
    when {
        row.isGranted -> Unit
        row.permission.permission == SHIZUKU_PERMISSION -> onRequestPermission()
        else -> destinations.openPermissionSettings(row.permission.permission)
    }
}
