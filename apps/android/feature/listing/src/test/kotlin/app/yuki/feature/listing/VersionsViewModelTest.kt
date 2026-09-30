package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.model.FailureReason
import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VersionsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val installGateway = RecordingInstallGateway()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModelWith(
        repository: FakeListingRepository,
    ): VersionsViewModel = VersionsViewModel(
        savedStateHandle = SavedStateHandle(mapOf(LISTING_SLUG_KEY to SLUG)),
        repository = repository,
        installGateway = installGateway,
    )

    private fun viewModelWith(result: Result<ListingDetail>): VersionsViewModel =
        viewModelWith(FakeListingRepository(result))

    @Test
    fun `lists every version of the listing`() = runTest {
        val versions = (1..5).map { index -> version("v$index.0.0") }
        val viewModel = viewModelWith(Result.success(detail(versions = versions)))

        viewModel.listing.test {
            assertEquals(UiState.Loading, awaitItem())
            val loaded = awaitItem() as UiState.Success

            assertEquals(versions, loaded.data.versions)
        }
    }

    @Test
    fun `retrying after a failure reloads the listing`() = runTest {
        val repository = FakeListingRepository(Result.failure(TypedFailure(FailureReason.Offline)))
        val viewModel = viewModelWith(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiState.Failure(FailureReason.Offline), viewModel.listing.value)

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, repository.detailCallCount)
    }

    @Test
    fun `installs the exact version the row asked for`() = runTest {
        val versions = listOf(version("v3.0.0"), version("v1.0.0"))
        val viewModel = viewModelWith(Result.success(detail(versions = versions)))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onVersionInstallAction(InstallAction.Install, requireNotNull(versions[1].toInstallable()))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("v1.0.0"), installGateway.requests.map { it.version.tag })
        assertEquals(42L, installGateway.requests.single().detail.githubRepoId)
    }

    @Test
    fun `cancels and opens through the listing install`() = runTest {
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()
        val installable = requireNotNull(version("v2.0.0").toInstallable())

        viewModel.onVersionInstallAction(InstallAction.Cancel, installable)
        viewModel.onVersionInstallAction(InstallAction.Open, installable)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(42L), installGateway.cancelled)
        assertEquals(listOf(42L), installGateway.opened)
    }

    @Test
    fun `a version row never triggers an uninstall`() = runTest {
        val viewModel = viewModelWith(Result.success(detail()))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onVersionInstallAction(
            InstallAction.Uninstall,
            requireNotNull(version("v2.0.0").toInstallable()),
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<Long>(), installGateway.uninstalled)
    }

    @Test
    fun `reports which version the gateway is installing`() = runTest {
        installGateway.emitObserved(InstallState.Installed("v2.0.0"), "v2.0.0")
        val viewModel = viewModelWith(Result.success(detail()))

        viewModel.installStatus.filterNotNull().test {
            assertEquals("v2.0.0", awaitItem().versionTag)
        }
    }
}
