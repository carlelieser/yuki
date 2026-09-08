package app.yuki.feature.settings

import android.Manifest

data class AppPermission(
    val permission: String,
    val label: String,
    val reason: String,
    val isRequired: Boolean,
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
    ),
    AppPermission(
        permission = Manifest.permission.POST_NOTIFICATIONS,
        label = "Notifications",
        reason = "Notify when a download or install finishes",
        isRequired = false,
    ),
    AppPermission(
        permission = SHIZUKU_PERMISSION,
        label = "Shizuku",
        reason = "Install silently through Shizuku",
        isRequired = false,
    ),
)
