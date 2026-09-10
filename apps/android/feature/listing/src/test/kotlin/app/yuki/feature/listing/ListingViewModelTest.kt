package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.model.FailureReason
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val installGateway = RecordingInstallGateway()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModelWith(result: Result<ListingDetail>): ListingViewModel =
        ListingViewModel(
            savedStateHandle = SavedStateHandle(mapOf(LISTING_SLUG_KEY to SLUG)),
            repository = FakeListingRepository(result),
            installGateway = installGateway,
        )

    @Test
    fun `emits loading then success carrying the installable version`() = runTest {
        val listing = detail(versions = listOf(version("v2.0.0-beta", isPrerelease = true), version("v1.5.0")))
        val viewModel = viewModelWith(Result.success(listing))

        viewModel.listing.test {
            assertEquals(UiState.Loading, awaitItem())

            val success = awaitItem() as UiState.Success
            assertEquals("v1.5.0", success.data.installableVersion?.tag)
            assertEquals(SLUG, success.data.detail.slug)
        }
    }

    @Test
    fun `emits loading then a not found failure for a missing slug`() = runTest {
        val viewModel = viewModelWith(Result.failure(TypedFailure(FailureReason.NotFound)))

        viewModel.listing.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Failure(FailureReason.NotFound), awaitItem())
        }
    }

    @Test
    fun `emits an offline failure when the network is unreachable`() = runTest {
        val viewModel = viewModelWith(Result.failure(TypedFailure(FailureReason.Offline)))

        viewModel.listing.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Failure(FailureReason.Offline), awaitItem())
        }
    }

    @Test
    fun `succeeds with no installable version when every release is a prerelease`() = runTest {
        val listing = detail(versions = listOf(version("v1.0.0-rc", isPrerelease = true)))
        val viewModel = viewModelWith(Result.success(listing))

        viewModel.listing.test {
            assertEquals(UiState.Loading, awaitItem())

            val success = awaitItem() as UiState.Success
            assertEquals(null, success.data.installableVersion)
            assertEquals(false, success.data.isInstallable)
        }
    }

    @Test
    fun `succeeds with no installable version when the version list is empty`() = runTest {
        val viewModel = viewModelWith(Result.success(detail(versions = emptyList())))

        viewModel.listing.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(false, (awaitItem() as UiState.Success).data.isInstallable)
        }
    }

    @Test
    fun `retrying after a failure reloads the listing`() = runTest {
        val viewModel = viewModelWith(Result.failure(TypedFailure(FailureReason.Server(500))))

        viewModel.listing.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Failure(FailureReason.Server(500)), awaitItem())

            viewModel.refresh()
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Failure(FailureReason.Server(500)), awaitItem())
        }
    }

    @Test
    fun `install action forwards the selected version to the gateway`() = runTest {
        val listing = detail(versions = listOf(version("v9.9.9-rc", isPrerelease = true), version("v9.0.0")))
        val viewModel = viewModelWith(Result.success(listing))

        viewModel.listing.test {
            awaitItem()
            awaitItem()
        }

        viewModel.onInstallAction(InstallAction.Install)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("v9.0.0", installGateway.requests.single().version.tag)
        assertEquals(42L, installGateway.requests.single().detail.githubRepoId)
    }

    @Test
    fun `install action is ignored when no version is installable`() = runTest {
        val viewModel = viewModelWith(Result.success(detail(versions = emptyList())))

        viewModel.listing.test {
            awaitItem()
            awaitItem()
        }

        viewModel.onInstallAction(InstallAction.Install)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<ListingInstallRequest>(), installGateway.requests)
    }

    @Test
    fun `cancel action reaches the gateway with the listing repo id`() = runTest {
        val viewModel = viewModelWith(Result.success(detail()))

        viewModel.listing.test {
            awaitItem()
            awaitItem()
        }

        viewModel.onInstallAction(InstallAction.Cancel)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(42L), installGateway.cancelled)
    }

    @Test
    fun `install progress observed for the listing reaches the screen`() = runTest {
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.installState.test {
            assertEquals(InstallState.NotInstalled, awaitItem())

            viewModel.onInstallAction(InstallAction.Install)
            dispatcher.scheduler.advanceUntilIdle()

            installGateway.emitObserved(InstallState.Downloading(HALF_DOWNLOADED))
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(InstallState.Downloading(HALF_DOWNLOADED), awaitItem())

            installGateway.emitObserved(InstallState.Installed("v2.0.0"))
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(InstallState.Installed("v2.0.0"), awaitItem())
        }
    }

    @Test
    fun `install keeps running after the screen stops collecting`() = runTest {
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.installState.test {
            assertEquals(InstallState.NotInstalled, awaitItem())
            viewModel.onInstallAction(InstallAction.Install)
            dispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, installGateway.requests.size)
        assertEquals(emptyList<Long>(), installGateway.cancelled)
    }

    @Test
    fun `install state follows the gateway once the listing loads`() = runTest {
        installGateway.emitObserved(InstallState.Installed("v2.0.0"))
        val viewModel = viewModelWith(Result.success(detail()))

        viewModel.installState.test {
            assertEquals(InstallState.NotInstalled, awaitItem())
            assertEquals(InstallState.Installed("v2.0.0"), awaitItem())
        }
    }
}

private val HALF_DOWNLOADED = downloadSizeOf(bytesDownloaded = 500L, bytesTotal = 1_000L)
