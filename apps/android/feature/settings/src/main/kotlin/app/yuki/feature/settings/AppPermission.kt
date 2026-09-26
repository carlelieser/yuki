package app.yuki.feature.settings

import android.Manifest
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
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

private val NETWORK = AppPermission(
    permission = Manifest.permission.INTERNET,
    label = R.string.settings_permission_network,
    reason = R.string.settings_permission_network_reason,
    isRequired = true,
)

private val INSTALL_PACKAGES = AppPermission(
    permission = Manifest.permission.REQUEST_INSTALL_PACKAGES,
    label = R.string.settings_permission_install,
    reason = R.string.settings_permission_install_reason,
    isRequired = true,
    kind = PermissionKind.InstallPackagesAppOp,
)

private val SHIZUKU = AppPermission(
    permission = SHIZUKU_PERMISSION,
    label = R.string.settings_permission_shizuku,
    reason = R.string.settings_permission_shizuku_reason,
    isRequired = false,
)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun notifications(): AppPermission = AppPermission(
    permission = Manifest.permission.POST_NOTIFICATIONS,
    label = R.string.settings_permission_notifications,
    isRequired = false,
)

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU, lambda = 1)
internal inline fun whenNotificationsExist(sdkInt: Int, block: () -> Unit) {
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) block()
}

fun yukiPermissions(sdkInt: Int): List<AppPermission> = buildList {
    add(NETWORK)
    add(INSTALL_PACKAGES)
    whenNotificationsExist(sdkInt) { add(notifications()) }
    add(SHIZUKU)
}

fun devicePermissions(): List<AppPermission> = yukiPermissions(Build.VERSION.SDK_INT)
