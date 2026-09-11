package app.yuki.feature.settings

import android.Manifest

enum class PermissionKind {
    Runtime,
    InstallPackagesAppOp,
}

data class AppPermission(
    val permission: String,
    val label: String,
    val isRequired: Boolean,
    val reason: String? = null,
    val kind: PermissionKind = PermissionKind.Runtime,
)

const val SHIZUKU_PERMISSION = "moe.shizuku.manager.permission.API_V23"

val YUKI_PERMISSIONS: List<AppPermission> = listOf(
    AppPermission(
        permission = Manifest.permission.INTERNET,
        label = "Network access",
        reason = "Browse listings and download apps",
        isRequired = true,
    ),
    AppPermission(
        permission = Manifest.permission.REQUEST_INSTALL_PACKAGES,
        label = "Install unknown apps",
        reason = "Install apps when Shizuku is unavailable",
        isRequired = true,
        kind = PermissionKind.InstallPackagesAppOp,
    ),
    AppPermission(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        label = "Notifications",
        isRequired = false,
    ),
    AppPermission(
        permission = SHIZUKU_PERMISSION,
        label = "Shizuku",
        reason = "Required to install apps without confirmation",
        isRequired = false,
    ),
)
