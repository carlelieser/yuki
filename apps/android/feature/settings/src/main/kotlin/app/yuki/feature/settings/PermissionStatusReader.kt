package app.yuki.feature.settings

import android.content.Context
import android.content.pm.PackageManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class PermissionStatus {
    Granted,
    Denied,
}

interface PermissionStatusReader {
    fun statusOf(permission: AppPermission): PermissionStatus
}

internal interface PlatformPermissions {
    fun isRuntimeGranted(permission: String): Boolean

    fun canRequestPackageInstalls(): Boolean
}

internal class ContextPlatformPermissions(private val context: Context) : PlatformPermissions {
    override fun isRuntimeGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    override fun canRequestPackageInstalls(): Boolean =
        context.packageManager.canRequestPackageInstalls()
}

@Singleton
internal class ContextPermissionStatusReader(
    private val platform: PlatformPermissions,
) : PermissionStatusReader {
    @Inject
    constructor(@ApplicationContext context: Context) : this(ContextPlatformPermissions(context))

    override fun statusOf(permission: AppPermission): PermissionStatus =
        if (isGranted(permission)) PermissionStatus.Granted else PermissionStatus.Denied

    private fun isGranted(permission: AppPermission): Boolean = when (permission.kind) {
        PermissionKind.Runtime -> platform.isRuntimeGranted(permission.permission)
        PermissionKind.InstallPackagesAppOp -> platform.canRequestPackageInstalls()
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PermissionStatusModule {
    @Binds
    @Singleton
    abstract fun bindReader(reader: ContextPermissionStatusReader): PermissionStatusReader
}
