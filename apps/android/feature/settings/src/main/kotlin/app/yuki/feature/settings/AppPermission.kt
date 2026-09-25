package app.yuki.feature.settings

import android.Manifest
import androidx.annotation.StringRes

enum class PermissionKind {
    Runtime,
    InstallPackagesAppOp,
}

data class AppPermission(
    val permission: String,
    @StringRes val label: Int,
    val isRequired: Boolean,
    @StringRes val reason: Int? = null,
    val kind: PermissionKind = PermissionKind.Runtime,
)

const val SHIZUKU_PERMISSION = "moe.shizuku.manager.permission.API_V23"

val YUKI_PERMISSIONS: List<AppPermission> = listOf(
    AppPermission(
        permission = Manifest.permission.INTERNET,
        label = R.string.settings_permission_network,
        reason = R.string.settings_permission_network_reason,
        isRequired = true,
    ),
    AppPermission(
        permission = Manifest.permission.REQUEST_INSTALL_PACKAGES,
        label = R.string.settings_permission_install,
        reason = R.string.settings_permission_install_reason,
        isRequired = true,
        kind = PermissionKind.InstallPackagesAppOp,
    ),
    AppPermission(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        label = R.string.settings_permission_notifications,
        isRequired = false,
    ),
    AppPermission(
        permission = SHIZUKU_PERMISSION,
        label = R.string.settings_permission_shizuku,
        reason = R.string.settings_permission_shizuku_reason,
        isRequired = false,
    ),
)
