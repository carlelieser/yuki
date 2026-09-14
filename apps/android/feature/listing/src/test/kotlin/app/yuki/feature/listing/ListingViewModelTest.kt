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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

        viewModel.installStatus.map { it.state }.test {
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

        viewModel.installStatus.map { it.state }.test {
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
    fun `installs the exact version the row asked for`() = runTest {
        val versions = listOf(version("v3.0.0"), version("v1.0.0"))
        val viewModel = viewModelWith(Result.success(detail(versions = versions)))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onVersionInstallAction(InstallAction.Install, version("v1.0.0"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("v1.0.0"), installGateway.requests.map { it.version.tag })
    }

    @Test
    fun `installs a prerelease when its own row asks for it`() = runTest {
        val prerelease = version("v4.0.0-rc", isPrerelease = true)
        val versions = listOf(prerelease, version("v3.0.0"))
        val viewModel = viewModelWith(Result.success(detail(versions = versions)))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onVersionInstallAction(InstallAction.Install, prerelease)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("v4.0.0-rc"), installGateway.requests.map { it.version.tag })
    }

    @Test
    fun `ignores a row install for a version without an asset`() = runTest {
        val missing = version("v2.0.0", downloadUrl = null)
        val viewModel = viewModelWith(Result.success(detail(versions = listOf(missing))))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onVersionInstallAction(InstallAction.Install, missing)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<ListingInstallRequest>(), installGateway.requests)
    }

    @Test
    fun `cancels the listing install when a version row cancels`() = runTest {
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onVersionInstallAction(InstallAction.Cancel, version("v2.0.0"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(42L), installGateway.cancelled)
    }

    @Test
    fun `reports which version the gateway is installing`() = runTest {
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.installStatus.test {
            assertEquals(null, awaitItem().versionTag)

            installGateway.emitObserved(InstallState.Downloading(HALF_DOWNLOADED), "v2.0.0")
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals("v2.0.0", awaitItem().versionTag)
        }
    }

    @Test
    fun `install state follows the gateway once the listing loads`() = runTest {
        installGateway.emitObserved(InstallState.Installed("v2.0.0"))
        val viewModel = viewModelWith(Result.success(detail()))

        viewModel.installStatus.map { it.state }.test {
            assertEquals(InstallState.NotInstalled, awaitItem())
            assertEquals(InstallState.Installed("v2.0.0"), awaitItem())
        }
    }

    @Test
    fun `asks before a silent uninstall instead of removing straight away`() = runTest {
        installGateway.isSilent = true
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onInstallAction(InstallAction.Uninstall)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.isConfirmingUninstall.value)
        assertTrue(installGateway.uninstalled.isEmpty())
    }

    @Test
    fun `uninstalls once the prompt is confirmed`() = runTest {
        installGateway.isSilent = true
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onInstallAction(InstallAction.Uninstall)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onUninstallConfirmed()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(42L), installGateway.uninstalled)
        assertFalse(viewModel.isConfirmingUninstall.value)
    }

    @Test
    fun `keeps the app when the prompt is dismissed`() = runTest {
        installGateway.isSilent = true
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onInstallAction(InstallAction.Uninstall)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onUninstallDismissed()

        assertFalse(viewModel.isConfirmingUninstall.value)
        assertTrue(installGateway.uninstalled.isEmpty())
    }

    @Test
    fun `defers to the system prompt without asking twice`() = runTest {
        installGateway.isSilent = false
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onInstallAction(InstallAction.Uninstall)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isConfirmingUninstall.value)
        assertEquals(listOf(42L), installGateway.uninstalled)
    }

    @Test
    fun `a version row never triggers an uninstall`() = runTest {
        installGateway.isSilent = true
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onVersionInstallAction(InstallAction.Uninstall, version("v1.5.0"))
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isConfirmingUninstall.value)
        assertTrue(installGateway.uninstalled.isEmpty())
    }

    @Test
    fun `surfaces a failed uninstall instead of crashing`() = runTest {
        installGateway.isSilent = true
        installGateway.uninstallError = IllegalStateException("pm refused")
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onInstallAction(InstallAction.Uninstall)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onUninstallConfirmed()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.hasUninstallFailed.value)
        assertEquals(1, installGateway.refreshes)
    }

    @Test
    fun `clears an earlier uninstall failure when trying again`() = runTest {
        installGateway.isSilent = false
        installGateway.uninstallError = IllegalStateException("pm refused")
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onInstallAction(InstallAction.Uninstall)
        dispatcher.scheduler.advanceUntilIdle()

        installGateway.uninstallError = null
        viewModel.onInstallAction(InstallAction.Uninstall)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.hasUninstallFailed.value)
    }

    @Test
    fun `refreshes install state when the screen resumes`() = runTest {
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onResumed()

        assertEquals(1, installGateway.refreshes)
    }
}

private val HALF_DOWNLOADED = downloadSizeOf(bytesDownloaded = 500L, bytesTotal = 1_000L)
