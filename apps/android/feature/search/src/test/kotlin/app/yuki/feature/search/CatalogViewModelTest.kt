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
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(list: String) = CatalogViewModel(
        savedStateHandle = SavedStateHandle(mapOf(CATALOG_LIST_KEY to list)),
        repository = repository,
        installedListings = FakeInstalledListings(),
        installProgress = FakeSearchProgressStore(),
    )

    @Test
    fun `the new list is titled New and ordered newest first`() = runTest {
        val model = viewModel("newest")
        model.listings.asSnapshot()

        assertEquals(R.string.search_catalog_newest_title, model.list.title)
        assertEquals(listOf("newest-desc"), repository.browsedSorts)
    }

    @Test
    fun `the updated list is titled Recently updated and ordered by latest release`() = runTest {
        val model = viewModel("updated")
        model.listings.asSnapshot()

        assertEquals(R.string.search_catalog_updated_title, model.list.title)
        assertEquals(listOf("updated-desc"), repository.browsedSorts)
    }

    @Test
    fun `browses the whole catalog rather than one category or author`() = runTest {
        viewModel("newest").listings.asSnapshot()

        assertEquals(listOf(null), repository.browsedCategories)
        assertEquals(listOf(null), repository.browsedAuthors)
    }

    @Test
    fun `an unknown list is rejected instead of silently reordering`() {
        val failure = assertThrows(IllegalStateException::class.java) { viewModel("loudest") }

        assertTrue(failure.message.orEmpty().contains("loudest"))
    }
}
