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
    fun statusOf(permission: String): PermissionStatus
}

internal fun isNotificationsSupported(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU

@Singleton
internal class ContextPermissionStatusReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PermissionStatusReader {
    override fun statusOf(permission: String): PermissionStatus {
        val isUnsupported = permission == Manifest.permission.POST_NOTIFICATIONS &&
            !isNotificationsSupported(Build.VERSION.SDK_INT)
        if (isUnsupported) return PermissionStatus.Granted

        return toStatus(context.checkSelfPermission(permission))
    }
}

internal fun toStatus(result: Int): PermissionStatus =
    if (result == PackageManager.PERMISSION_GRANTED) {
        PermissionStatus.Granted
    } else {
        PermissionStatus.Denied
    }

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PermissionStatusModule {
    @Binds
    @Singleton
    abstract fun bindReader(reader: ContextPermissionStatusReader): PermissionStatusReader
}
