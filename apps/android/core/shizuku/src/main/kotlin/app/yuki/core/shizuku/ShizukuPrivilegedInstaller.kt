package app.yuki.core.shizuku

import app.yuki.core.installer.InstallOutcome
import app.yuki.core.installer.InstallStrategy
import app.yuki.core.installer.PrivilegedInstaller
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuPrivilegedInstaller @Inject internal constructor(
    private val monitor: ShizukuMonitor,
    private val silent: ShizukuInstallStrategy,
) : PrivilegedInstaller {
    override suspend fun isReady(): Boolean = monitor.isReady()

    override fun strategy(): InstallStrategy = silent

    override suspend fun uninstall(packageName: String): InstallOutcome =
        silent.uninstall(packageName)
}
