package app.yuki.install

import app.yuki.core.database.InstallStore
import app.yuki.core.installer.InstalledListings
import app.yuki.core.model.InstalledApp
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@Singleton
internal class StoreInstalledListings @Inject constructor(
    private val store: InstallStore,
    private val presence: InstalledPackagePresence,
) : InstalledListings {
    override fun observeInstalledIds(): Flow<Set<Long>> =
        combine(store.observeInstalls(), presence.observeChanges()) { installs, _ ->
            installs.filter(::isStillPresent).mapTo(mutableSetOf(), InstalledApp::githubRepoId)
        }.distinctUntilChanged()

    private fun isStillPresent(app: InstalledApp): Boolean = presence.isPresent(app.packageName)
}
