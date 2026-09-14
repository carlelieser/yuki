package app.yuki.install

import app.yuki.core.database.InstallStore
import app.yuki.core.installer.InstalledListings
import app.yuki.core.model.InstalledApp
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class StoreInstalledListings @Inject constructor(
    private val store: InstallStore,
    private val presence: InstalledPackagePresence,
) : InstalledListings {
    override fun observeInstalledIds(): Flow<Set<Long>> =
        store.observeInstalls().map { installs ->
            installs.filter(::isStillPresent).mapTo(mutableSetOf(), InstalledApp::githubRepoId)
        }

    private fun isStillPresent(app: InstalledApp): Boolean = presence.isPresent(app.packageName)
}
