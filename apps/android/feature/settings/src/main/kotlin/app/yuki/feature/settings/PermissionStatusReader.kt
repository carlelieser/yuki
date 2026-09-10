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

internal fun interface PlatformPermissionLookup {
    fun isGranted(kind: PermissionKind, permission: String): Boolean
}

internal fun isNotificationsSupported(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU

internal fun statusOf(
    permission: AppPermission,
    sdkInt: Int,
    lookup: PlatformPermissionLookup,
): PermissionStatus {
    val isUnsupported = permission.permission == Manifest.permission.POST_NOTIFICATIONS &&
        !isNotificationsSupported(sdkInt)
    if (isUnsupported) return PermissionStatus.Granted

    return if (lookup.isGranted(permission.kind, permission.permission)) {
        PermissionStatus.Granted
    } else {
        PermissionStatus.Denied
    }
}

@Singleton
internal class ContextPermissionStatusReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PermissionStatusReader {
    private val lookup = PlatformPermissionLookup { kind, permission ->
        when (kind) {
            PermissionKind.Runtime -> isRuntimeGranted(permission)
            PermissionKind.InstallPackagesAppOp -> context.packageManager.canRequestPackageInstalls()
        }
    }

    override fun statusOf(permission: AppPermission): PermissionStatus =
        statusOf(permission, Build.VERSION.SDK_INT, lookup)

    private fun isRuntimeGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PermissionStatusModule {
    @Binds
    @Singleton
    abstract fun bindReader(reader: ContextPermissionStatusReader): PermissionStatusReader
}
