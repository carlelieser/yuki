package app.yuki.core.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import rikka.shizuku.Shizuku

interface UserServiceBinder {
    suspend fun <T> withInstaller(block: suspend (IYukiInstaller) -> T): T
}

@Singleton
internal class ShizukuUserServiceBinder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val versions: UserServiceVersion,
) : UserServiceBinder {
    private val lock = Mutex()
    private var bound: IYukiInstaller? = null

    private val arguments: Shizuku.UserServiceArgs
        get() = Shizuku.UserServiceArgs(
            ComponentName(context.packageName, YukiInstallerService::class.java.name),
        )
            .daemon(false)
            .processNameSuffix("installer")
            .version(versions.code)

    override suspend fun <T> withInstaller(block: suspend (IYukiInstaller) -> T): T = lock.withLock {
        val installer = liveInstaller() ?: bind()
        bound = installer

        block(installer)
    }

    private fun liveInstaller(): IYukiInstaller? =
        bound?.takeIf { installer -> installer.asBinder().pingBinder() }

    private suspend fun bind(): IYukiInstaller = withInstallTimeout(
        USER_SERVICE_BIND_TIMEOUT,
        "The Shizuku installer service did not connect within $USER_SERVICE_BIND_TIMEOUT",
    ) {
        suspendCancellableCoroutine { continuation ->
            val connection = ServiceConnectionAdapter(continuation) { bound = null }
            continuation.invokeOnCancellation {
                Shizuku.unbindUserService(arguments, connection, true)
            }
            Shizuku.bindUserService(arguments, connection)
        }
    }
}

private class ServiceConnectionAdapter(
    private val continuation: CancellableContinuation<IYukiInstaller>,
    private val onLost: () -> Unit,
) : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        val installer = binder
            ?.takeIf { bound -> bound.pingBinder() }
            ?.let(IYukiInstaller.Stub::asInterface)

        if (installer == null) {
            continuation.resumeIfActive(
                PrivilegedInstallException("Shizuku bound a dead installer binder for $name"),
            )
            return
        }

        if (continuation.isActive) continuation.resume(installer)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        onLost()
        continuation.resumeIfActive(
            PrivilegedInstallException("The Shizuku installer service disconnected for $name"),
        )
    }
}

private fun CancellableContinuation<IYukiInstaller>.resumeIfActive(error: Exception) {
    if (isActive) resumeWith(Result.failure(error))
}
