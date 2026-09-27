package app.yuki.feature.explore

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingPage
import app.yuki.core.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreRowsTest {
    private val repository = FakeListingRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        ExploreViewModel(repository, FakeInstalledListings(), FakeExploreProgressStore())

    private fun page(vararg slugs: String) =
        Result.success(ListingPage(slugs.map(::listing), hasMore = false))

    @Test
    fun `asks the server for a short row of newest and recently updated listings`() = runTest {
        viewModel()
        advanceUntilIdle()

        val requested = repository.browsedQueries.map { query -> query.sort to query.limit }
        assertEquals(
            listOf("newest" to ROW_ITEM_COUNT, "updated" to ROW_ITEM_COUNT),
            requested,
        )
    }

    @Test
    fun `shows the newest and recently updated listings the server returns`() = runTest {
        repository.browseResultsBySort["newest"] = page("fresh")
        repository.browseResultsBySort["updated"] = page("patched")

        val model = viewModel()
        advanceUntilIdle()

        model.state.test {
            val content = awaitContent()
            assertEquals(listOf(listing("fresh")), (content.newest as UiState.Success).data)
            assertEquals(listOf(listing("patched")), (content.updated as UiState.Success).data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a loaded row keeps the screen up when featured and sections fail`() = runTest {
        repository.failEverySource(FailureReason.Offline)
        repository.browseResultsBySort["updated"] = page("patched")

        val model = viewModel()
        advanceUntilIdle()

        model.state.test {
            val content = awaitContent()
            assertEquals(UiState.Failure(FailureReason.Offline), content.newest)
            assertTrue(content.updated is UiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed pull keeps the rows that were already loaded`() = runTest {
        repository.browseResultsBySort["newest"] = page("fresh")

        val model = viewModel()
        advanceUntilIdle()

        repository.failEverySource(FailureReason.Offline)
        repository.browseResultsBySort.clear()
        model.onPullToRefresh()
        advanceUntilIdle()

        model.state.test {
            val content = awaitContent()
            assertEquals(listOf(listing("fresh")), (content.newest as UiState.Success).data)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private suspend fun ReceiveTurbine<UiState<ExploreContent>>.awaitContent(): ExploreContent {
    while (true) {
        val state = awaitItem()
        if (state is UiState.Success) return state.data
    }
}
