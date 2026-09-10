package app.yuki.feature.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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

    val sdkInt: Int
}

internal fun isNotificationsSupported(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU

internal class ContextPlatformPermissions(private val context: Context) : PlatformPermissions {
    override fun isRuntimeGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    override fun canRequestPackageInstalls(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    override val sdkInt: Int get() = Build.VERSION.SDK_INT
}

@Singleton
internal class ContextPermissionStatusReader(
    private val platform: PlatformPermissions,
) : PermissionStatusReader {
    @Inject
    constructor(@ApplicationContext context: Context) : this(ContextPlatformPermissions(context))

    override fun statusOf(permission: AppPermission): PermissionStatus {
        if (isUnsupportedNotifications(permission)) return PermissionStatus.Granted

        return if (isGranted(permission)) PermissionStatus.Granted else PermissionStatus.Denied
    }

    private fun isUnsupportedNotifications(permission: AppPermission): Boolean =
        permission.permission == Manifest.permission.POST_NOTIFICATIONS &&
            !isNotificationsSupported(platform.sdkInt)

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
