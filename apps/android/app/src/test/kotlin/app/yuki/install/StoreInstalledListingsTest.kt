package app.yuki.install

import app.yuki.core.database.InstallRecording
import app.yuki.core.database.InstallStore
import app.yuki.core.model.InstalledApp
import app.yuki.feature.library.DevicePackage
import app.yuki.feature.library.InstalledPackages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreInstalledListingsTest {
    @Test
    fun `drops the listing when its package leaves the device without a store write`() = runTest {
        val packages = FakePackages(present = mutableSetOf(TERMUX.packageName))
        val listings = listingsFor(FixedInstallStore(listOf(TERMUX)), packages)
        val seen = mutableListOf<Set<Long>>()
        val collecting = launch { listings.observeInstalledIds().toList(seen) }

        runCurrent()
        packages.remove(TERMUX.packageName)
        runCurrent()
        collecting.cancel()

        assertEquals(listOf(setOf(TERMUX.githubRepoId), emptySet<Long>()), seen)
    }

    @Test
    fun `reports a listing whose package is on the device`() = runTest {
        val packages = FakePackages(present = mutableSetOf(TERMUX.packageName))
        val listings = listingsFor(FixedInstallStore(listOf(TERMUX)), packages)

        assertEquals(setOf(TERMUX.githubRepoId), listings.observeInstalledIds().first())
    }

    @Test
    fun `ignores a recorded listing that was never present`() = runTest {
        val listings = listingsFor(FixedInstallStore(listOf(TERMUX)), FakePackages())

        assertEquals(emptySet<Long>(), listings.observeInstalledIds().first())
    }
}

private fun listingsFor(store: InstallStore, packages: InstalledPackages) =
    StoreInstalledListings(store, InstalledPackagePresence(packages))

private val TERMUX = InstalledApp(
    githubRepoId = 1_234L,
    packageName = "com.termux",
    slug = "termux",
    title = "Termux",
    iconUrl = null,
    versionTag = "v0.118.0",
)

private class FakePackages(
    private val present: MutableSet<String> = mutableSetOf(),
) : InstalledPackages {
    private val changes = MutableStateFlow(0)

    override fun isPresent(packageName: String): Boolean = packageName in present

    override fun launchIntentExists(packageName: String): Boolean = packageName in present

    override fun findAll(packageNames: List<String>): List<DevicePackage> = emptyList()

    override fun observeChanges(): Flow<Unit> = changes.map { }

    fun remove(packageName: String) {
        present -= packageName
        changes.value += 1
    }
}

private class FixedInstallStore(private val apps: List<InstalledApp>) : InstallStore {
    override fun observeInstalls(): Flow<List<InstalledApp>> = MutableStateFlow(apps)

    override suspend fun installs(): List<InstalledApp> = apps

    override suspend fun packageNameOf(githubRepoId: Long): String? =
        apps.firstOrNull { app -> app.githubRepoId == githubRepoId }?.packageName

    override suspend fun record(recording: InstallRecording) = Unit

    override suspend fun forget(githubRepoId: Long) = Unit

    override suspend fun forgetPackages(packageNames: List<String>) = Unit
}
