package app.yuki.core.shizuku

import android.os.ParcelFileDescriptor
import kotlin.system.exitProcess

class YukiInstallerService : IYukiInstaller.Stub() {
    private val sessions = PrivilegedSessions(ProcessShellCommandRunner())

    override fun install(
        apk: ParcelFileDescriptor,
        packageName: String,
        size: Long,
        installerPackage: String,
        callback: IInstallCallback,
    ) = report(callback) {
        val target = InstallTarget(
            packageName = packageName,
            size = size,
            installerPackage = installerPackage,
        )

        ParcelFileDescriptor.AutoCloseInputStream(apk).use { stream ->
            sessions.install(target) { sink -> stream.copyTo(sink) }
        }
    }

    override fun uninstall(packageName: String, callback: IInstallCallback) = report(callback) {
        sessions.uninstall(packageName)
    }

    override fun destroy() {
        exitProcess(0)
    }
}

private inline fun report(callback: IInstallCallback, work: () -> Unit) {
    try {
        work()
        callback.onFinished(INSTALL_STATUS_SUCCESS, null)
    } catch (error: PrivilegedInstallException) {
        callback.onFinished(INSTALL_STATUS_FAILURE, error.message)
    } catch (error: RuntimeException) {
        callback.onFinished(INSTALL_STATUS_FAILURE, error.toString())
    }
}
