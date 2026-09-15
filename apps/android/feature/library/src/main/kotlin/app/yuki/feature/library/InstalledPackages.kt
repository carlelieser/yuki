package app.yuki.feature.library

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface InstalledPackages {
    fun isPresent(packageName: String): Boolean

    fun launchIntentExists(packageName: String): Boolean

    fun findAll(packageNames: List<String>): List<DevicePackage>
}

@Singleton
internal class PackageManagerPresence @Inject constructor(
    private val packageManager: PackageManager,
) : InstalledPackages {
    override fun isPresent(packageName: String): Boolean = infoFor(packageName) != null

    override fun launchIntentExists(packageName: String): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) != null

    override fun findAll(packageNames: List<String>): List<DevicePackage> =
        packageNames.mapNotNull { packageName ->
            infoFor(packageName)?.let { info ->
                DevicePackage(
                    packageName = packageName,
                    versionName = info.versionName.orEmpty(),
                    versionCode = info.longVersionCode(),
                    firstInstalledAt = Instant.ofEpochMilli(info.firstInstallTime),
                )
            }
        }

    private fun infoFor(packageName: String): PackageInfo? = try {
        packageManager.getPackageInfo(packageName, 0)
    } catch (absent: PackageManager.NameNotFoundException) {
        null
    }
}

private fun PackageInfo.longVersionCode(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }

@Module
@InstallIn(SingletonComponent::class)
internal object InstalledPackagesModule {
    @Provides
    @Singleton
    fun packageManager(@ApplicationContext context: Context): PackageManager =
        context.packageManager

    @Provides
    @Singleton
    fun installedPackages(implementation: PackageManagerPresence): InstalledPackages =
        implementation
}
