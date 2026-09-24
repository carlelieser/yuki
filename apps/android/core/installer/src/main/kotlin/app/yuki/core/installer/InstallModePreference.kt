package app.yuki.core.installer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

enum class PreferredInstaller { Privileged, System }

fun interface InstallModePreference {
    fun preferredInstaller(): Flow<PreferredInstaller>
}

internal class InstallModeGatedInstaller(
    private val inner: PrivilegedInstaller,
    private val preference: InstallModePreference,
) : PrivilegedInstaller {
    override suspend fun isReady(): Boolean =
        preference.preferredInstaller().first() == PreferredInstaller.Privileged && inner.isReady()

    override fun strategy(): InstallStrategy = inner.strategy()

    override suspend fun uninstall(packageName: String): InstallOutcome =
        inner.uninstall(packageName)
}
