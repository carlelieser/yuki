package app.yuki.core.shizuku

enum class ShizukuState {
    NotInstalled,
    NotRunning,
    PermissionRequired,
    Ready,
}

enum class ShizukuMode {
    AdbShell,
    Root,
    Unknown,
}

data class ShizukuDetail(
    val state: ShizukuState,
    val mode: ShizukuMode,
    val apiVersion: Int,
) {
    companion object {
        val Unavailable: ShizukuDetail =
            ShizukuDetail(ShizukuState.NotInstalled, ShizukuMode.Unknown, UNKNOWN_API_VERSION)
    }
}

const val UNKNOWN_API_VERSION: Int = -1

internal const val ADB_SHELL_UID: Int = 2000
internal const val ROOT_UID: Int = 0

internal fun uidToMode(uid: Int): ShizukuMode = when (uid) {
    ADB_SHELL_UID -> ShizukuMode.AdbShell
    ROOT_UID -> ShizukuMode.Root
    else -> ShizukuMode.Unknown
}
