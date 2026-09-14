package app.yuki.core.shizuku

import android.os.ParcelFileDescriptor
import app.yuki.core.installer.ApkIdentity
import app.yuki.core.installer.InstallOutcome
import app.yuki.core.installer.InstallStrategy
import app.yuki.core.model.InstallFailure
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine

internal class ShizukuInstallStrategy @Inject constructor(
    private val binder: UserServiceBinder,
) : InstallStrategy {
    override fun install(apk: File, identity: ApkIdentity): Flow<InstallOutcome> = flow {
        emit(runInstall(apk, identity))
    }

    suspend fun uninstall(packageName: String): InstallOutcome = binder.withInstaller { installer ->
        awaitCallback { callback -> installer.uninstall(packageName, callback) }
    }

    private suspend fun runInstall(apk: File, identity: ApkIdentity): InstallOutcome =
        openDescriptor(apk).use { descriptor ->
            binder.withInstaller { installer ->
                awaitCallback { callback ->
                    installer.install(descriptor, identity.packageName, apk.length(), callback)
                }
            }
        }

    private fun openDescriptor(apk: File): ParcelFileDescriptor = try {
        ParcelFileDescriptor.open(apk, ParcelFileDescriptor.MODE_READ_ONLY)
    } catch (error: FileNotFoundException) {
        throw PrivilegedInstallException("Could not open ${apk.absolutePath} for Shizuku", error)
    }
}

internal suspend fun awaitCallback(
    request: (IInstallCallback) -> Unit,
): InstallOutcome = withInstallTimeout(
    PRIVILEGED_INSTALL_TIMEOUT,
    "The privileged installer did not report a result within $PRIVILEGED_INSTALL_TIMEOUT",
) {
    suspendCancellableCoroutine { continuation ->
        val callback = object : IInstallCallback.Stub() {
            override fun onFinished(status: Int, message: String?) {
                if (continuation.isActive) continuation.resume(toOutcome(status, message))
            }
        }

        request(callback)
    }
}

internal fun toOutcome(status: Int, message: String?): InstallOutcome =
    if (status == INSTALL_STATUS_SUCCESS) {
        InstallOutcome.Succeeded
    } else {
        InstallOutcome.Failed(toFailure(message))
    }

private fun toFailure(message: String?): InstallFailure {
    val detail = message?.takeIf(String::isNotBlank) ?: return InstallFailure.Aborted

    return when {
        detail.contains("INSUFFICIENT_STORAGE") -> InstallFailure.InsufficientStorage
        detail.contains("SIGNATURES") || detail.contains("ALREADY_EXISTS") ->
            InstallFailure.PackageMismatch
        detail.contains("INCOMPATIBLE") || detail.contains("INVALID_APK") ->
            InstallFailure.Incompatible
        else -> InstallFailure.Rejected(detail)
    }
}
