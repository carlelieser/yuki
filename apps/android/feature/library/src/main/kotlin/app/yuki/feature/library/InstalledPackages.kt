package app.yuki.feature.library

import android.content.Context
import android.content.pm.PackageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface InstalledPackages {
    fun isPresent(packageName: String): Boolean

    fun launchIntentExists(packageName: String): Boolean
}

@Singleton
internal class PackageManagerPresence @Inject constructor(
    private val packageManager: PackageManager,
) : InstalledPackages {
    override fun isPresent(packageName: String): Boolean = try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (absent: PackageManager.NameNotFoundException) {
        false
    }

    override fun launchIntentExists(packageName: String): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) != null
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
