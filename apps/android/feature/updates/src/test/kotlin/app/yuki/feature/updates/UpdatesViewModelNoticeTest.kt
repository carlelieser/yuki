package app.yuki.feature.updates

import app.cash.turbine.test
import app.yuki.core.model.UiState
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
class UpdatesViewModelNoticeTest {
    private val dispatcher = StandardTestDispatcher()
    private val pending = FakePendingUpdateStore()
    private val notifier = RecordingUpdateNotifier()

    @Before
    fun installDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun updatesShownOnScreenAreRecordedAsSeen() = runTest {
        val viewModel = viewModelWithUpdate()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            awaitItem()

            assertEquals(listOf(TERMUX.githubRepoId), pending.rows.map { row -> row.githubRepoId })
            assertTrue(pending.unnotified().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun openingUpdatesDismissesTheUpdatesNotification() = runTest {
        val viewModel = viewModelWithUpdate()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            awaitItem()

            assertEquals(1, notifier.dismissals)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun viewModelWithUpdate(): UpdatesViewModel {
        val detail = listingDetail(TERMUX.githubRepoId, TERMUX.slug, listOf(version("v0.119.0")))
        val repository = FakeListingRepository(mapOf(TERMUX.slug to Result.success(detail)))

        return UpdatesViewModel(
            store = FakeInstallStore(listOf(TERMUX)),
            dependencies = UpdatesDependencies(
                check = UpdateCheck(repository, pending) { flowOf(false) },
                installer = RecordingUpdateInstaller(),
                notifier = notifier,
            ),
        )
    }
}

private val TERMUX = installedApp(1_234L, "termux", "v0.118.0")
