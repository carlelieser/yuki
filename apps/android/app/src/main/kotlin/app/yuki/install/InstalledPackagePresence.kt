package app.yuki.install

import app.yuki.feature.library.InstalledPackages
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
internal class InstalledPackagePresence @Inject constructor(
    private val packages: InstalledPackages,
) {
    fun isPresent(packageName: String): Boolean = packages.isPresent(packageName)

    fun observeChanges(): Flow<Unit> = packages.observeChanges()
}
