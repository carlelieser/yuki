package app.yuki.core.shizuku

import kotlinx.coroutines.flow.Flow

const val SHELL_INSTALLER_PACKAGE = "shell"

fun interface InstallerPackagePreference {
    fun installerPackage(): Flow<String>
}
