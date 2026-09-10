package app.yuki.feature.updates

import app.cash.turbine.test
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.model.FailureReason
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingVersion
import app.yuki.core.model.UiState
import app.yuki.core.network.RemoteRequestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class UpdatesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val installer = RecordingUpdateInstaller()

    @Before
    fun installDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun reportsAnUpdateWhenAListingHasANewerVersion() = runTest {
        val viewModel = viewModelFor(
            installs = listOf(TERMUX),
            details = mapOf(TERMUX.slug to Result.success(termuxWith(version("v0.119.0")))),
        )

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            val content = successOf(awaitItem())

            assertEquals(listOf(TERMUX.githubRepoId), content.updates.map(UpdateRow::githubRepoId))
            assertEquals("v0.119.0", content.updates.single().update.version.tag)
            assertTrue(content.unchecked.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun findingNoUpdatesIsASuccessNotAFailure() = runTest {
        val viewModel = viewModelFor(
            installs = listOf(TERMUX),
            details = mapOf(TERMUX.slug to Result.success(termuxWith(version("v0.118.0")))),
        )

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            val content = successOf(awaitItem())

            assertTrue(content.isEmpty)
            assertTrue(content.hasNoUpdates)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noInstalledAppsIsASuccessNotAFailure() = runTest {
        val viewModel = viewModelFor(installs = emptyList(), details = emptyMap())

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertTrue(successOf(awaitItem()).isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun oneFailingListingLeavesOtherUpdatesVisible() = runTest {
        val viewModel = viewModelFor(
            installs = listOf(TERMUX, AURORA),
            details = mapOf(
                TERMUX.slug to Result.success(termuxWith(version("v0.119.0"))),
                AURORA.slug to Result.failure(offline("Load listing detail for slug=aurora-store")),
            ),
        )

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            val content = successOf(awaitItem())

            assertEquals(listOf(TERMUX.githubRepoId), content.updates.map(UpdateRow::githubRepoId))
            assertEquals(listOf(AURORA.githubRepoId), content.unchecked.map(UncheckedApp::githubRepoId))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aFailingListingKeepsItsTypedReasonOnTheRow() = runTest {
        val viewModel = viewModelFor(
            installs = listOf(AURORA),
            details = mapOf(AURORA.slug to Result.failure(notFound())),
        )

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            val content = successOf(awaitItem())

            assertEquals(FailureReason.NotFound, content.unchecked.single().reason)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun everyListingFailingIsStillASuccessWithPerRowFailures() = runTest {
        val viewModel = viewModelFor(
            installs = listOf(TERMUX, AURORA),
            details = mapOf(
                TERMUX.slug to Result.failure(offline("Load listing detail for slug=termux")),
                AURORA.slug to Result.failure(offline("Load listing detail for slug=aurora-store")),
            ),
        )

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            val content = successOf(awaitItem())

            assertTrue(content.hasNoUpdates)
            assertEquals(2, content.unchecked.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun prereleasesAreHiddenWhenThePreferenceIsOff() = runTest {
        val viewModel = viewModelFor(
            installs = listOf(TERMUX),
            details = mapOf(
                TERMUX.slug to Result.success(termuxWith(version("v0.119.0-beta1", isPrerelease = true))),
            ),
            includePrereleases = false,
        )

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertTrue(successOf(awaitItem()).hasNoUpdates)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun prereleasesAreOfferedWhenThePreferenceIsOn() = runTest {
        val viewModel = viewModelFor(
            installs = listOf(TERMUX),
            details = mapOf(
                TERMUX.slug to Result.success(termuxWith(version("v0.119.0-beta1", isPrerelease = true))),
            ),
            includePrereleases = true,
        )

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            val content = successOf(awaitItem())

            assertEquals("v0.119.0-beta1", content.updates.single().update.version.tag)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aRowStartsAsUpdateAvailableAndFollowsTheInstaller() = runTest {
        val viewModel = viewModelFor(
            installs = listOf(TERMUX),
            details = mapOf(TERMUX.slug to Result.success(termuxWith(version("v0.119.0")))),
        )

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(
                InstallState.UpdateAvailable(from = "v0.118.0", to = "v0.119.0"),
                successOf(awaitItem()).updates.single().install,
            )

            viewModel.onInstallAction(TERMUX.githubRepoId, InstallAction.Update)
            installer.emit(InstallState.Downloading(HALF_DOWNLOADED))

            assertEquals(
                InstallState.Downloading(HALF_DOWNLOADED),
                successOf(awaitItem()).updates.single().install,
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf("v0.119.0"), installer.requested.map { it.version.tag })
    }

    private fun viewModelFor(
        installs: List<InstalledApp>,
        details: Map<String, Result<ListingDetail>>,
        includePrereleases: Boolean = false,
    ): UpdatesViewModel = UpdatesViewModel(
        store = FakeInstallStore(installs),
        dependencies = UpdatesDependencies(
            check = UpdateCheck(FakeListingRepository(details)),
            installer = installer,
            preference = { flowOf(includePrereleases) },
        ),
    )
}

private fun successOf(state: UiState<UpdatesContent>): UpdatesContent =
    (state as UiState.Success).data

private fun offline(operation: String): Throwable =
    RemoteRequestException(FailureReason.Offline, operation)

private fun notFound(): Throwable =
    RemoteRequestException(FailureReason.NotFound, "Load listing detail for slug=aurora-store")

private fun termuxWith(vararg versions: ListingVersion): ListingDetail =
    listingDetail(TERMUX.githubRepoId, TERMUX.slug, versions.toList())

private val TERMUX = installedApp(1_234L, "termux", "v0.118.0")

private val AURORA = installedApp(5_678L, "aurora-store", "4.6.4")

private val HALF_DOWNLOADED = downloadSizeOf(bytesDownloaded = 500L, bytesTotal = 1_000L)
