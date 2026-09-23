package app.yuki.core.shizuku

import android.os.IBinder
import android.os.ParcelFileDescriptor

internal class RecordingUserServiceBinder : UserServiceBinder {
    val installer: RecordingInstaller = RecordingInstaller()

    override suspend fun <T> withInstaller(block: suspend (IYukiInstaller) -> T): T =
        block(installer)
}

internal class RecordingInstaller : IYukiInstaller {
    val installerPackages: MutableList<String> = mutableListOf()
    val packageNames: MutableList<String> = mutableListOf()

    override fun install(
        apk: ParcelFileDescriptor?,
        packageName: String?,
        size: Long,
        installerPackage: String?,
        callback: IInstallCallback?,
    ) {
        packageNames += packageName.orEmpty()
        installerPackages += installerPackage.orEmpty()
        callback?.onFinished(INSTALL_STATUS_SUCCESS, null)
    }

    override fun uninstall(packageName: String?, callback: IInstallCallback?) {
        packageNames += packageName.orEmpty()
        callback?.onFinished(INSTALL_STATUS_SUCCESS, null)
    }

    override fun destroy() = Unit

    override fun asBinder(): IBinder =
        throw UnsupportedOperationException("The recording installer has no binder")
}
