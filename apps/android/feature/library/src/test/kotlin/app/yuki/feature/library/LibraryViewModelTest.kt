package app.yuki.feature.library

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.yuki.core.database.InstallRecording
import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.UiState
import app.yuki.core.model.downloadSizeOf
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            assertEquals(listOf(TERMUX.slug), successSlugs(awaitItem()))
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
            assertEquals(listOf(TERMUX.slug), successSlugs(awaitItem()))
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
    fun reconcilesAgainOnRefreshWhenAnAppDisappears() = runTest {
        val store = FakeInstallStore(listOf(TERMUX))
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val viewModel = viewModelFor(store, packages)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(listOf(TERMUX.slug), successSlugs(awaitItem()))

            packages.uninstall(TERMUX.packageName)
            viewModel.onPullToRefresh()

            assertEquals(emptyList<String>(), successSlugs(awaitItem()))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aStoreEmissionDoesNotWriteBackToTheStore() = runTest {
        val store = FakeInstallStore(listOf(TERMUX))
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val viewModel = viewModelFor(store, packages)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(listOf(TERMUX.slug), successSlugs(awaitItem()))

            val afterLoad = store.forgetCalls

            packages.install(AURORA.packageName)
            store.record(recordingOf(AURORA))
            advanceUntilIdle()

            assertEquals(afterLoad, store.forgetCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reconcilingWritesOnlyWhenSomethingIsActuallyGone() = runTest {
        val store = FakeInstallStore(listOf(TERMUX))
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val viewModel = viewModelFor(store, packages)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(listOf(TERMUX.slug), successSlugs(awaitItem()))

            viewModel.onPullToRefresh()
            advanceUntilIdle()

            assertEquals(0, store.forgetCalls)
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
                    TERMUX.target,
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
            assertEquals(
                LibrarySupporting.Version(TERMUX.versionTag),
                singleItem(awaitItem()).supporting,
            )

            progress.write(
                InstallProgress(
                    TERMUX.target,
                    "v0.119.0",
                    InstallState.Downloading(HALF_DOWNLOADED),
                ),
            )

            assertEquals(
                LibrarySupporting.Download(HALF_DOWNLOADED),
                singleItem(awaitItem()).supporting,
            )
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
                    AURORA.target,
                    "v4.7.0",
                    InstallState.Downloading(HALF_DOWNLOADED),
                ),
            )

            val items = (awaitItem() as UiState.Success).data
            assertEquals(listOf(AURORA.githubRepoId), items.downloading.map(LibraryItem::githubRepoId))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aFirstTimeDownloadAppearsEvenThoughNothingIsInstalledYet() = runTest {
        val progress = FakeLibraryProgressStore()
        val viewModel = viewModelFor(FakeInstallStore(), FakeInstalledPackages(), progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertTrue((awaitItem() as UiState.Success).data.isEmpty)

            progress.write(
                InstallProgress(OBSIDIAN, "v1.5.0", InstallState.Downloading(HALF_DOWNLOADED)),
            )

            val item = singleItem(awaitItem())
            assertEquals("Obsidian", item.entry.title)
            assertEquals(LibrarySupporting.Download(HALF_DOWNLOADED), item.supporting)
            assertTrue(item.isDownloading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aFirstTimeDownloadKeepsItsSlugSoTheRowStillOpensTheListing() = runTest {
        val progress = FakeLibraryProgressStore()
        val viewModel = viewModelFor(FakeInstallStore(), FakeInstalledPackages(), progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            awaitItem()

            progress.write(
                InstallProgress(OBSIDIAN, "v1.5.0", InstallState.Downloading(HALF_DOWNLOADED)),
            )

            assertEquals("obsidian", singleItem(awaitItem()).slug)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aCompletedFirstInstallBecomesASingleInstalledRow() = runTest {
        val store = FakeInstallStore()
        val packages = FakeInstalledPackages()
        val progress = FakeLibraryProgressStore()
        val viewModel = viewModelFor(store, packages, progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            awaitItem()

            progress.write(
                InstallProgress(OBSIDIAN, "v1.5.0", InstallState.Downloading(HALF_DOWNLOADED)),
            )
            assertEquals(OBSIDIAN.githubRepoId, singleItem(awaitItem()).githubRepoId)

            packages.install(OBSIDIAN_APP.packageName)
            progress.write(InstallProgress(OBSIDIAN, "v1.5.0", InstallState.Installed("v1.5.0")))
            store.record(recordingOf(OBSIDIAN_APP))

            val settled = awaitLatestItems(this)
            assertEquals(listOf(OBSIDIAN.githubRepoId), settled.map(LibraryItem::githubRepoId))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aFailedFirstTimeDownloadStaysVisibleSoItCanBeRetried() = runTest {
        val progress = FakeLibraryProgressStore()
        val viewModel = viewModelFor(FakeInstallStore(), FakeInstalledPackages(), progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            awaitItem()

            progress.write(
                InstallProgress(
                    OBSIDIAN,
                    "v1.5.0",
                    InstallState.Failed(InstallFailure.InsufficientStorage),
                ),
            )

            val item = singleItem(awaitItem())
            assertTrue(item.isFailed)
            assertEquals(InstallFailure.InsufficientStorage, item.failure)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aDownloadSortsAboveTheAppsThatAreAlreadySettled() = runTest {
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val progress = FakeLibraryProgressStore()
        val viewModel = viewModelFor(FakeInstallStore(listOf(TERMUX)), packages, progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            awaitItem()

            progress.write(
                InstallProgress(OBSIDIAN, "v1.5.0", InstallState.Downloading(HALF_DOWNLOADED)),
            )

            val items = (awaitItem() as UiState.Success).data.items
            assertEquals(
                listOf(OBSIDIAN.githubRepoId, TERMUX.githubRepoId),
                items.map(LibraryItem::githubRepoId),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun anInstalledAppBeingUpdatedNeverRendersTwice() = runTest {
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val progress = FakeLibraryProgressStore()
        val viewModel = viewModelFor(FakeInstallStore(listOf(TERMUX)), packages, progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            awaitItem()

            progress.write(
                InstallProgress(TERMUX.target, "v0.119.0", InstallState.Downloading(HALF_DOWNLOADED)),
            )

            val items = (awaitItem() as UiState.Success).data.items
            assertEquals(listOf(TERMUX.githubRepoId), items.map(LibraryItem::githubRepoId))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun settledInstallProgressIsClearedSoRowsDoNotLeakForever() = runTest {
        val progress = FakeLibraryProgressStore()
        val viewModel = viewModelFor(FakeInstallStore(), FakeInstalledPackages(), progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(progress.settledClearances > 0)
    }

    @Test
    fun aFailedInstallSurvivesAResumeSoTheUserCanSeeWhatWentWrong() = runTest {
        val progress = FakeLibraryProgressStore()
        progress.write(
            InstallProgress(OBSIDIAN, "v1.5.0", InstallState.Failed(InstallFailure.InsufficientStorage)),
        )
        val viewModel = viewModelFor(FakeInstallStore(), FakeInstalledPackages(), progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertTrue(singleItem(awaitItem()).isFailed)

            viewModel.onPullToRefresh()
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(progress.find(OBSIDIAN.githubRepoId)?.state is InstallState.Failed)
    }

    @Test
    fun aFailedInstallSurvivesAPullToRefresh() = runTest {
        val progress = FakeLibraryProgressStore()
        val viewModel = viewModelFor(FakeInstallStore(), FakeInstalledPackages(), progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            awaitItem()

            progress.write(
                InstallProgress(OBSIDIAN, "v1.5.0", InstallState.Failed(InstallFailure.InsufficientStorage)),
            )
            assertTrue(singleItem(awaitItem()).isFailed)

            viewModel.onPullToRefresh()
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(progress.find(OBSIDIAN.githubRepoId)?.state is InstallState.Failed)
    }

    @Test
    fun dismissingAFailedInstallRemovesTheRow() = runTest {
        val progress = FakeLibraryProgressStore()
        progress.write(
            InstallProgress(OBSIDIAN, "v1.5.0", InstallState.Failed(InstallFailure.InsufficientStorage)),
        )
        val viewModel = viewModelFor(FakeInstallStore(), FakeInstalledPackages(), progress)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertTrue(singleItem(awaitItem()).isFailed)

            viewModel.onDismiss(OBSIDIAN.githubRepoId)
            assertTrue(content(awaitItem()).isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aPullReconcilesAgainWhenAnAppDisappeared() = runTest {
        val store = FakeInstallStore(listOf(TERMUX))
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val viewModel = viewModelFor(store, packages)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(listOf(TERMUX.slug), successSlugs(awaitItem()))

            packages.uninstall(TERMUX.packageName)
            viewModel.onPullToRefresh()

            assertEquals(emptyList<String>(), successSlugs(awaitItem()))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aPullKeepsTheIndicatorUpLongEnoughToBeSeen() = runTest {
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val viewModel = viewModelFor(FakeInstallStore(listOf(TERMUX)), packages)
        advanceUntilIdle()

        viewModel.onPullToRefresh()
        runCurrent()

        assertTrue(viewModel.isRefreshing.value)

        advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun aPullKeepsTheInstalledAppsOnScreenThroughout() = runTest {
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val viewModel = viewModelFor(FakeInstallStore(listOf(TERMUX)), packages)

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(listOf(TERMUX.slug), successSlugs(awaitItem()))

            viewModel.onPullToRefresh()
            runCurrent()

            assertTrue(viewModel.state.value is UiState.Success)

            advanceUntilIdle()
            assertTrue(viewModel.state.value is UiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aSecondPullIsIgnoredWhileOneIsAlreadyRunning() = runTest {
        val packages = FakeInstalledPackages().apply { install(TERMUX.packageName) }
        val progress = FakeLibraryProgressStore()
        val viewModel = viewModelFor(FakeInstallStore(listOf(TERMUX)), packages, progress)
        advanceUntilIdle()

        viewModel.onPullToRefresh()
        runCurrent()
        val afterFirst = progress.settledClearances

        viewModel.onPullToRefresh()
        runCurrent()

        assertEquals(afterFirst, progress.settledClearances)

        advanceUntilIdle()
    }

    @Test
    fun concurrentDownloadsKeepTheOrderTheStoreGaveThem() = runTest {
        val progress = FakeLibraryProgressStore()
        val targets = listOf(TERMUX, AURORA, OBSIDIAN_APP)
        targets.forEach { app ->
            progress.write(downloadingProgress(app, bytesDownloaded = 100L))
        }
        val viewModel = viewModelFor(
            FakeInstallStore(emptyList()),
            FakeInstalledPackages(),
            progress,
        )

        viewModel.state.test {
            awaitUntilIds(this, targets.map(InstalledApp::githubRepoId))

            progress.write(downloadingProgress(AURORA, bytesDownloaded = 900L))

            assertEquals(
                targets.map(InstalledApp::githubRepoId),
                awaitLatestIds(this),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aRowStaysInPlaceWhenItsDownloadFinishesAndInstallBegins() = runTest {
        val progress = FakeLibraryProgressStore()
        val targets = listOf(TERMUX, AURORA)
        targets.forEach { app ->
            progress.write(downloadingProgress(app, bytesDownloaded = 100L))
        }
        val viewModel = viewModelFor(
            FakeInstallStore(emptyList()),
            FakeInstalledPackages(),
            progress,
        )

        viewModel.state.test {
            awaitUntilIds(this, targets.map(InstalledApp::githubRepoId))

            progress.write(
                InstallProgress(TERMUX.target, TERMUX.versionTag, InstallState.Installing),
            )

            assertEquals(
                targets.map(InstalledApp::githubRepoId),
                awaitLatestIds(this),
            )
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
        dependencies = libraryDependencies(store, packages),
    )
}

private suspend fun awaitLatestItems(
    turbine: ReceiveTurbine<UiState<LibraryContent>>,
): List<LibraryItem> {
    while (true) {
        val next = turbine.awaitItem() as? UiState.Success ?: continue
        val items = next.data.items
        if (items.isNotEmpty() && items.all { item -> item.isInstalled }) return items
    }
}

private fun downloadingProgress(app: InstalledApp, bytesDownloaded: Long): InstallProgress =
    InstallProgress(
        target = app.target,
        versionTag = app.versionTag,
        state = InstallState.Downloading(
            downloadSizeOf(bytesDownloaded = bytesDownloaded, bytesTotal = 1_000L),
        ),
    )

private suspend fun awaitUntilIds(
    turbine: ReceiveTurbine<UiState<LibraryContent>>,
    expected: List<Long>,
) {
    while (true) {
        val next = turbine.awaitItem() as? UiState.Success ?: continue
        if (next.data.items.map(LibraryItem::githubRepoId) == expected) return
    }
}

private suspend fun awaitLatestIds(
    turbine: ReceiveTurbine<UiState<LibraryContent>>,
): List<Long> {
    while (true) {
        val next = turbine.awaitItem() as? UiState.Success ?: continue
        return next.data.items.map(LibraryItem::githubRepoId)
    }
}

private fun singleItem(state: UiState<LibraryContent>): LibraryItem =
    (state as UiState.Success).data.items.single()

private fun content(state: UiState<LibraryContent>): LibraryContent =
    (state as UiState.Success).data

private val OBSIDIAN_APP = InstalledApp(
    githubRepoId = 9_012L,
    packageName = "md.obsidian",
    slug = "obsidian",
    title = "Obsidian",
    iconUrl = null,
    versionTag = "v1.5.0",
)

private fun recordingOf(app: InstalledApp): InstallRecording =
    InstallRecording(app = app, versionCode = 1L, installedAt = Instant.EPOCH)

private val HALF_DOWNLOADED = downloadSizeOf(bytesDownloaded = 500L, bytesTotal = 1_000L)

private val InstalledApp.target: InstallTarget
    get() = InstallTarget(githubRepoId, slug, title, iconUrl)

private val OBSIDIAN = InstallTarget(
    githubRepoId = 9_012L,
    slug = "obsidian",
    title = "Obsidian",
    iconUrl = null,
)

private fun successSlugs(state: UiState<LibraryContent>): List<String> =
    (state as UiState.Success).data.items.map(LibraryItem::slug)

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
