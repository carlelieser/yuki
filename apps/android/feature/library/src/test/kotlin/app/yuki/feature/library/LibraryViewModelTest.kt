package app.yuki.feature.library

import app.cash.turbine.test
import app.yuki.core.installer.InstallProgress
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.UiState
import app.yuki.core.model.downloadSizeOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun installDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun emitsLoadingThenTheInstalledApps() = runTest {
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val viewModel = viewModelFor(FakeInstallStore(listOf(TERMUX)), packages)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(listOf(TERMUX), successApps(awaitItem()))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dropsAppsTheUserUninstalledOutsideYuki() = runTest {
        val store = FakeInstallStore(listOf(TERMUX, AURORA))
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val viewModel = viewModelFor(store, packages)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(listOf(TERMUX), successApps(awaitItem()))
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf(listOf(AURORA.packageName)), store.forgottenPackages)
    }

    @Test
    fun anEmptyLibraryIsASuccessNotAFailure() = runTest {
        val viewModel = viewModelFor(FakeInstallStore(), FakeInstalledPackages())

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            val content = (awaitItem() as UiState.Success).data
            assertTrue(content.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reconcilesAgainOnResumeWhenAnAppDisappears() = runTest {
        val store = FakeInstallStore(listOf(TERMUX))
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val viewModel = viewModelFor(store, packages)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(listOf(TERMUX), successApps(awaitItem()))

            packages.uninstall(TERMUX.packageName)
            viewModel.onResume()

            assertEquals(emptyList<InstalledApp>(), successApps(awaitItem()))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun marksAnAppOpenableOnlyWhenItHasALaunchIntent() = runTest {
        val packages = FakeInstalledPackages().apply {
            install(TERMUX.packageName, isLaunchable = true)
            install(AURORA.packageName, isLaunchable = false)
        }
        val viewModel = viewModelFor(FakeInstallStore(listOf(TERMUX, AURORA)), packages)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            val items = (awaitItem() as UiState.Success).data.items
            assertEquals(listOf(true, false), items.map(LibraryItem::canOpen))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun showsAnInFlightDownloadWithItsByteCounts() = runTest {
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val progress = FakeLibraryProgressStore()
        val viewModel = viewModelFor(FakeInstallStore(listOf(TERMUX)), packages, progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(InstallState.NotInstalled, singleItem(awaitItem()).install)

            progress.write(
                InstallProgress(
                    TERMUX.githubRepoId,
                    "v0.119.0",
                    InstallState.Downloading(HALF_DOWNLOADED),
                ),
            )

            val downloading = singleItem(awaitItem())
            assertEquals(InstallState.Downloading(HALF_DOWNLOADED), downloading.install)
            assertTrue(downloading.isDownloading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aDownloadingRowShowsTransferredBytesInsteadOfTheInstalledVersion() = runTest {
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val progress = FakeLibraryProgressStore()
        val viewModel = viewModelFor(FakeInstallStore(listOf(TERMUX)), packages, progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(TERMUX.versionTag, singleItem(awaitItem()).listItem.supporting)

            progress.write(
                InstallProgress(
                    TERMUX.githubRepoId,
                    "v0.119.0",
                    InstallState.Downloading(HALF_DOWNLOADED),
                ),
            )

            assertEquals("500 B / 1 KB", singleItem(awaitItem()).listItem.supporting)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aDownloadIsAttributedOnlyToTheAppItBelongsTo() = runTest {
        val packages = FakeInstalledPackages().apply {
            install(TERMUX.packageName)
            install(AURORA.packageName)
        }
        val progress = FakeLibraryProgressStore()
        val store = FakeInstallStore(listOf(TERMUX, AURORA))
        val viewModel = viewModelFor(store, packages, progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            awaitItem()

            progress.write(
                InstallProgress(
                    AURORA.githubRepoId,
                    "v4.7.0",
                    InstallState.Downloading(HALF_DOWNLOADED),
                ),
            )

            val items = (awaitItem() as UiState.Success).data
            assertEquals(listOf(AURORA.githubRepoId), items.downloading.map(LibraryItem::githubRepoId))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModelFor(
        store: FakeInstallStore,
        packages: FakeInstalledPackages,
        progress: FakeLibraryProgressStore = FakeLibraryProgressStore(),
    ): LibraryViewModel = LibraryViewModel(
        store = store,
        progress = progress,
        dependencies = LibraryDependencies(LibraryReconciler(store, packages), packages),
    )
}

private fun singleItem(state: UiState<LibraryContent>): LibraryItem =
    (state as UiState.Success).data.items.single()

private val HALF_DOWNLOADED = downloadSizeOf(bytesDownloaded = 500L, bytesTotal = 1_000L)

private fun successApps(state: UiState<LibraryContent>): List<InstalledApp> =
    (state as UiState.Success).data.items.map(LibraryItem::app)

private val TERMUX = InstalledApp(
    githubRepoId = 1_234L,
    packageName = "com.termux",
    slug = "termux",
    title = "Termux",
    iconUrl = null,
    versionTag = "v0.118.0",
)

private val AURORA = InstalledApp(
    githubRepoId = 5_678L,
    packageName = "com.aurora.store",
    slug = "aurora-store",
    title = "Aurora Store",
    iconUrl = null,
    versionTag = "4.6.4",
)
