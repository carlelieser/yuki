package app.yuki.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.paging.testing.asSnapshot
import app.yuki.core.model.ListingPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    private val repository = FakeListingRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(sort: String) = CatalogViewModel(
        savedStateHandle = SavedStateHandle(mapOf(CATALOG_SORT_KEY to sort)),
        repository = repository,
        installedListings = FakeInstalledListings(),
        installProgress = FakeSearchProgressStore(),
    )

    @Test
    fun `opens newest listings first when entered from the new row`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        val model = viewModel("newest")
        model.listings.asSnapshot()

        assertEquals(BrowseSortOption.NewestFirst, model.sort.value)
        assertEquals(listOf("newest-desc"), repository.browsedSorts)
    }

    @Test
    fun `opens recently updated listings first when entered from the updated row`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        val model = viewModel("updated")
        model.listings.asSnapshot()

        assertEquals(BrowseSortOption.RecentlyUpdated, model.sort.value)
        assertEquals(listOf("updated-desc"), repository.browsedSorts)
    }

    @Test
    fun `browses the whole catalog rather than one category or author`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        viewModel("newest").listings.asSnapshot()

        assertEquals(listOf(null), repository.browsedCategories)
        assertEquals(listOf(null), repository.browsedAuthors)
    }

    @Test
    fun `an unknown sort is rejected instead of silently reordering`() {
        val failure = assertThrows(IllegalStateException::class.java) { viewModel("loudest") }

        assertTrue(failure.message.orEmpty().contains("loudest"))
    }
}
