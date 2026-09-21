package app.yuki.core.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.ShizukuProvider

internal const val API_V23_SUFFIX: String = ".permission.API_V23"

internal fun isForkApiPermission(name: String): Boolean = name.endsWith(API_V23_SUFFIX)

internal fun Context.declaresAnyShizukuApiPermission(): Boolean =
    declaresShizukuApiPermission() || declaresForkApiPermission()

private fun Context.declaresShizukuApiPermission(): Boolean = try {
    packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0) != null
} catch (missing: PackageManager.NameNotFoundException) {
    false
}

private fun Context.declaresForkApiPermission(): Boolean = packageManager
    .getInstalledPackages(PackageManager.GET_PERMISSIONS)
    .any { installed ->
        installed.permissions.orEmpty().any { permission -> isForkApiPermission(permission.name) }
    }
