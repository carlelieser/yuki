package app.yuki.feature.search

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingPage
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
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

    private fun viewModel(
        installed: FakeInstalledListings = FakeInstalledListings(),
    ) = SearchViewModel(
        repository = repository,
        recentSearches = recentSearches,
        installedListings = installed,
    )

    @Test
    fun `search results know which listings are installed`() = runTest {
        val alpha = listing("alpha").githubRepoId
        repository.searchResult = Result.success(listOf(listing("alpha"), listing("beta")))

        val model = viewModel(FakeInstalledListings(setOf(alpha)))

        model.state.test {
            awaitContent()

            model.onQueryChange("alpha")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)

            val content = awaitSearched()
            assertTrue(alpha in content.installedIds)
            assertFalse(listing("beta").githubRepoId in content.installedIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `installed ids are exposed for the paged browse list`() = runTest {
        val alpha = listing("alpha").githubRepoId

        val model = viewModel(FakeInstalledListings(setOf(alpha)))

        model.installedIds.test {
            assertEquals(setOf(alpha), awaitNonEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `browse starts unfiltered by category`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        viewModel().listings.collectPage()

        assertEquals(listOf(null), repository.browsedCategories)
    }

    @Test
    fun `changing the category refetches from offset zero`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        val model = viewModel()
        model.listings.collectPage()
        repository.browsedOffsets.clear()
        repository.browsedCategories.clear()

        model.onCategoryChange(ListingCategory.Media)
        model.listings.collectPage()

        assertEquals(listOf(0), repository.browsedOffsets)
        assertEquals(listOf(ListingCategory.Media), repository.browsedCategories)
    }

    @Test
    fun `changing the sort refetches from offset zero with the new sort`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        val model = viewModel()
        model.listings.collectPage()
        repository.browsedOffsets.clear()
        repository.browsedSorts.clear()

        model.onSortChange(BrowseSortOption.NameAscending)
        model.listings.collectPage()

        assertEquals(listOf(0), repository.browsedOffsets)
        assertEquals(listOf("name-asc"), repository.browsedSorts)
    }

    @Test
    fun `a later page keeps the active category and sort`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), true))

        val model = viewModel()
        model.onCategoryChange(ListingCategory.Gaming)
        model.onSortChange(BrowseSortOption.NameAscending)
        model.listings.collectPage()

        assertTrue(repository.browsedOffsets.contains(BROWSE_PAGE_SIZE))
        assertTrue(repository.browsedCategories.all { it == ListingCategory.Gaming })
        assertTrue(repository.browsedSorts.all { sort -> sort == "name-asc" })
    }

    @Test
    fun `the default browse sort is stars descending`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        viewModel().listings.collectPage()

        assertEquals(listOf("stars-desc"), repository.browsedSorts)
    }

    @Test
    fun `an empty query shows the browse list rather than search results`() = runTest {
        val model = viewModel()

        model.state.test {
            val content = awaitContent()
            assertNull(content.results)
            assertTrue(!content.isSearching)
            cancelAndIgnoreRemainingEvents()
        }

        assertTrue(repository.searchedQueries.isEmpty())
    }

    @Test
    fun `a non-empty query shows search results`() = runTest {
        repository.searchResult = Result.success(listOf(listing("beta")))

        val model = viewModel()
        model.state.test {
            awaitContent()

            model.onQueryChange("beta")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)

            val results = awaitSearchResults() as UiState.Success
            assertEquals(listOf(listing("beta")), results.data)
            assertEquals(listOf("beta"), repository.searchedQueries)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a successful search is remembered as a recent search`() = runTest {
        repository.searchResult = Result.success(listOf(listing("beta")))

        val model = viewModel()
        model.state.test {
            awaitContent()

            model.onQueryChange("beta")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            awaitSearchResults()

            assertEquals(listOf("beta"), recentSearches.snapshot())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed search is not remembered`() = runTest {
        repository.searchResult = Result.failure(TypedFailure(FailureReason.Offline))

        val model = viewModel()
        model.state.test {
            awaitContent()

            model.onQueryChange("beta")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)

            val outcome = awaitSearchResults()
            assertEquals(UiState.Failure(FailureReason.Offline), outcome)
            assertTrue(recentSearches.snapshot().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a query longer than the server cap is truncated before the request`() = runTest {
        repository.searchResult = Result.success(emptyList())

        val model = viewModel()
        model.state.test {
            awaitContent()

            model.onQueryChange("q".repeat(MAXIMUM_QUERY_LENGTH + 50))
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            awaitSearchResults()

            assertEquals(MAXIMUM_QUERY_LENGTH, repository.searchedQueries.single().length)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `debounce collapses rapid keystrokes into one request`() = runTest {
        repository.searchResult = Result.success(listOf(listing("beta")))

        val model = viewModel()
        model.state.test {
            awaitContent()

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
    fun `changing the sort refetches the active search with the new sort`() = runTest {
        repository.searchResult = Result.success(listOf(listing("beta")))

        val model = viewModel()
        model.state.test {
            awaitContent()

            model.onQueryChange("beta")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            awaitSearchResults()

            assertEquals(listOf("stars-desc"), repository.searchedSorts)

            model.onSortChange(BrowseSortOption.NameAscending)
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            awaitSearchResults()

            assertEquals(listOf("stars-desc", "name-asc"), repository.searchedSorts)
            assertEquals(listOf("beta", "beta"), repository.searchedQueries)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the active sort travels with the search request`() = runTest {
        repository.searchResult = Result.success(listOf(listing("beta")))

        val model = viewModel()
        model.onSortChange(BrowseSortOption.NewestFirst)

        model.state.test {
            awaitContent()

            model.onQueryChange("beta")
            advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
            awaitSearchResults()

            assertEquals(listOf("newest-desc"), repository.searchedSorts)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a removed recent search is forgotten`() = runTest {
        recentSearches.remember("beta")

        val model = viewModel()
        model.onRecentSearchRemoved("beta")

        model.state.test {
            while (awaitContent().recent.isNotEmpty()) continue
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private suspend fun Flow<PagingData<ListingSummary>>.collectPage(): List<ListingSummary> =
    asSnapshot()

private suspend fun ReceiveTurbine<UiState<SearchContent>>.awaitContent(): SearchContent {
    while (true) {
        val state = awaitItem()
        if (state is UiState.Success) return state.data
    }
}

private suspend fun ReceiveTurbine<UiState<SearchContent>>.awaitSearched(): SearchContent {
    while (true) {
        val content = awaitContent()
        if (content.results is UiState.Success) return content
    }
}

private suspend fun ReceiveTurbine<Set<Long>>.awaitNonEmpty(): Set<Long> {
    while (true) {
        val ids = awaitItem()
        if (ids.isNotEmpty()) return ids
    }
}

private suspend fun ReceiveTurbine<UiState<SearchContent>>.awaitSearchResults():
    UiState<List<*>> {
    while (true) {
        val results = awaitContent().results
        if (results != null && results !is UiState.Loading) return results
    }
}
