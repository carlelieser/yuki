package app.yuki.feature.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
)

interface InstalledAppsReader {
    suspend fun read(): List<InstalledApp>

    suspend fun labelOf(packageName: String): String?

    suspend fun iconOf(packageName: String): Drawable?
}

@Singleton
internal class PackageManagerInstalledAppsReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : InstalledAppsReader {
    private val packages: PackageManager get() = context.packageManager

    override suspend fun read(): List<InstalledApp> = withContext(Dispatchers.IO) {
        packages.getInstalledApplications(0)
            .map(::toInstalledApp)
            .sortedBy { app -> app.label.lowercase() }
    }

    override suspend fun labelOf(packageName: String): String? = withContext(Dispatchers.IO) {
        installedInfo(packageName)?.let { info -> packages.getApplicationLabel(info).toString() }
    }

    override suspend fun iconOf(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        installedInfo(packageName)?.let(packages::getApplicationIcon)
    }

    private fun installedInfo(packageName: String): ApplicationInfo? = try {
        packages.getApplicationInfo(packageName, 0)
    } catch (error: PackageManager.NameNotFoundException) {
        null
    }

    private fun toInstalledApp(info: ApplicationInfo): InstalledApp = InstalledApp(
        packageName = info.packageName,
        label = packages.getApplicationLabel(info).toString(),
    )
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class InstalledAppsReaderModule {
    @Binds
    @Singleton
    abstract fun bindReader(reader: PackageManagerInstalledAppsReader): InstalledAppsReader
}

internal fun matchingApps(apps: List<InstalledApp>, query: String): List<InstalledApp> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return apps

    return apps.filter { app -> app.matches(trimmed) }
}

private fun InstalledApp.matches(query: String): Boolean =
    label.contains(query, ignoreCase = true) || packageName.contains(query, ignoreCase = true)
