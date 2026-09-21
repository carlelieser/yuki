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
class AuthorViewModelTest {
    private val repository = FakeListingRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        author: String = "acme",
        installed: FakeInstalledListings = FakeInstalledListings(),
        progress: FakeSearchProgressStore = FakeSearchProgressStore(),
    ) = AuthorViewModel(
        savedStateHandle = SavedStateHandle(mapOf(AUTHOR_KEY to author)),
        repository = repository,
        installedListings = installed,
        installProgress = progress,
    )

    @Test
    fun `the route author reaches the repository`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        viewModel("acme").listings.asSnapshot()

        assertEquals(listOf("acme"), repository.browsedAuthors)
    }

    @Test
    fun `the route author is exposed as the screen title source`() {
        assertEquals("nullpointer", viewModel("nullpointer").author)
    }

    @Test
    fun `the author browse is not narrowed to a category`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        viewModel("acme").listings.asSnapshot()

        assertTrue(repository.browsedCategories.all { it == null })
    }

    @Test
    fun `a blank author is rejected instead of browsing everything`() {
        val failure = assertThrows(IllegalStateException::class.java) {
            AuthorViewModel(
                savedStateHandle = SavedStateHandle(mapOf(AUTHOR_KEY to "   ")),
                repository = repository,
                installedListings = FakeInstalledListings(),
                installProgress = FakeSearchProgressStore(),
            )
        }

        assertTrue(failure.message.orEmpty().contains("without an author"))
    }

    @Test
    fun `a missing author is rejected instead of browsing everything`() {
        assertThrows(IllegalStateException::class.java) {
            AuthorViewModel(
                savedStateHandle = SavedStateHandle(),
                repository = repository,
                installedListings = FakeInstalledListings(),
                installProgress = FakeSearchProgressStore(),
            )
        }
    }

    @Test
    fun `changing the sort refetches from offset zero within the same author`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        val model = viewModel("acme")
        model.listings.asSnapshot()
        repository.browsedOffsets.clear()
        repository.browsedSorts.clear()
        repository.browsedAuthors.clear()

        model.onSortChange(BrowseSortOption.NameAscending)
        model.listings.asSnapshot()

        assertEquals(listOf(0), repository.browsedOffsets)
        assertEquals(listOf("name-asc"), repository.browsedSorts)
        assertEquals(listOf("acme"), repository.browsedAuthors)
    }

    @Test
    fun `the default sort is stars descending`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        val model = viewModel()
        model.listings.asSnapshot()

        assertEquals(BrowseSortOption.MostStars, model.sort.value)
        assertEquals(listOf("stars-desc"), repository.browsedSorts)
    }

    @Test
    fun `a later page stays inside the author`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), true))

        viewModel("yuki").listings.asSnapshot()

        assertTrue(repository.browsedOffsets.contains(BROWSE_PAGE_SIZE))
        assertTrue(repository.browsedAuthors.all { it == "yuki" })
    }

    @Test
    fun `a server that ignores the filter yields nothing rather than every app`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), true))

        val snapshot = viewModel("someone-else").listings.asSnapshot()

        assertTrue(snapshot.isEmpty())
    }
}
