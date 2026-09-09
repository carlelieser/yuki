package app.yuki.feature.explore

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.yuki.core.model.FailureReason
import app.yuki.core.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {
    private val repository = FakeListingRepository()
    private val recentSearches = FakeRecentSearchStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ExploreViewModel(repository, recentSearches)

    @Test
    fun `emits loading then success once featured resolves`() = runTest {
        repository.featuredResult = Result.success(listOf(listing("alpha")))

        viewModel().state.test {
            assertEquals(UiState.Loading, awaitItem())

            val loaded = awaitItem() as UiState.Success
            assertEquals(listOf(listing("alpha")), (loaded.data.featured as UiState.Success).data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits loading then failure when featured fails`() = runTest {
        repository.featuredResult = Result.failure(TypedFailure(FailureReason.Offline))

        viewModel().state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Failure(FailureReason.Offline), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an empty featured list is a success not a failure`() = runTest {
        repository.featuredResult = Result.success(emptyList())

        viewModel().state.test {
            assertEquals(UiState.Loading, awaitItem())

            val loaded = awaitItem() as UiState.Success
            val featured = loaded.data.featured as UiState.Success
            assertTrue(featured.data.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an empty search is distinguishable from a failed search`() = runTest {
        repository.featuredResult = Result.success(listOf(listing("alpha")))
        repository.searchResult = Result.success(emptyList())

        val model = viewModel()
        model.state.test {
            skipItems(1)
            awaitItem()

            model.onQueryChange("ghost")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)

            val outcome = awaitSearchResults()
            assertEquals(UiState.Success(emptyList<Nothing>()), outcome)
            cancelAndIgnoreRemainingEvents()
        }

        repository.searchResult = Result.failure(TypedFailure(FailureReason.Server(500)))

        val failing = viewModel()
        failing.state.test {
            skipItems(1)
            awaitItem()

            failing.onQueryChange("ghost")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)

            val outcome = awaitSearchResults()
            assertEquals(UiState.Failure(FailureReason.Server(500)), outcome)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a query longer than the server cap is truncated before the request`() = runTest {
        repository.featuredResult = Result.success(listOf(listing("alpha")))
        repository.searchResult = Result.success(emptyList())

        val model = viewModel()
        model.state.test {
            skipItems(1)
            awaitItem()

            model.onQueryChange("q".repeat(MAXIMUM_QUERY_LENGTH + 50))
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            awaitSearchResults()

            assertEquals(MAXIMUM_QUERY_LENGTH, repository.searchedQueries.single().length)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search below the minimum length leaves results absent`() = runTest {
        repository.featuredResult = Result.success(listOf(listing("alpha")))

        val model = viewModel()
        model.state.test {
            skipItems(1)
            val loaded = awaitItem() as UiState.Success
            assertNull(loaded.data.search.results)

            model.onQueryChange("")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            expectNoEvents()

            assertTrue(repository.searchedQueries.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `debounce collapses rapid keystrokes into one request`() = runTest {
        repository.featuredResult = Result.success(listOf(listing("alpha")))
        repository.searchResult = Result.success(listOf(listing("beta")))

        val model = viewModel()
        model.state.test {
            skipItems(1)
            awaitItem()

            listOf("b", "be", "bet", "beta").forEach { value ->
                model.onQueryChange(value)
                advanceTimeBy(SEARCH_DEBOUNCE_MILLIS / 5)
            }
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)

            awaitSearchResults()
            assertEquals(listOf("beta"), repository.searchedQueries)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a successful search is remembered as a recent search`() = runTest {
        repository.featuredResult = Result.success(listOf(listing("alpha")))
        repository.searchResult = Result.success(listOf(listing("beta")))

        val model = viewModel()
        model.state.test {
            skipItems(1)
            awaitItem()

            model.onQueryChange("beta")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            val outcome = awaitSearchResults()

            assertTrue(outcome is UiState.Success)
            assertEquals(listOf("beta"), recentSearches.snapshot())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed search is not remembered`() = runTest {
        repository.featuredResult = Result.success(listOf(listing("alpha")))
        repository.searchResult = Result.failure(TypedFailure(FailureReason.Offline))

        val model = viewModel()
        model.state.test {
            skipItems(1)
            awaitItem()

            model.onQueryChange("beta")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            val outcome = awaitSearchResults()

            assertTrue(outcome is UiState.Failure)
            assertTrue(recentSearches.snapshot().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retrying featured recovers from a failure`() = runTest {
        repository.featuredResult = Result.failure(TypedFailure(FailureReason.Offline))

        val model = viewModel()
        model.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Failure(FailureReason.Offline), awaitItem())

            repository.featuredResult = Result.success(listOf(listing("alpha")))
            model.refreshFeatured()

            val loaded = awaitItem() as UiState.Success
            assertEquals(1, (loaded.data.featured as UiState.Success).data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private suspend fun ReceiveTurbine<UiState<ExploreContent>>.awaitSearchResults():
    UiState<List<*>> {
    while (true) {
        val results = searchResultsOf(awaitItem())
        if (results != null && results !is UiState.Loading) return results
    }
}

private fun searchResultsOf(state: UiState<ExploreContent>): UiState<List<*>>? =
    (state as? UiState.Success)?.data?.search?.results
