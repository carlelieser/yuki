package app.yuki.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.paging.testing.asSnapshot
import app.yuki.core.model.ListingCategory
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
class CategoryViewModelTest {
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
        category: ListingCategory = ListingCategory.Gaming,
        installed: FakeInstalledListings = FakeInstalledListings(),
    ) = CategoryViewModel(
        savedStateHandle = SavedStateHandle(mapOf(CATEGORY_KEY to category.wireValue)),
        repository = repository,
        installedListings = installed,
    )

    @Test
    fun `the route category reaches the repository`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        viewModel(ListingCategory.Gaming).listings.asSnapshot()

        assertEquals(listOf(ListingCategory.Gaming), repository.browsedCategories)
    }

    @Test
    fun `the route category is exposed as the screen title source`() {
        assertEquals(ListingCategory.Media, viewModel(ListingCategory.Media).category)
    }

    @Test
    fun `an unknown category is rejected instead of browsing everything`() {
        val failure = assertThrows(IllegalStateException::class.java) {
            CategoryViewModel(
                savedStateHandle = SavedStateHandle(mapOf(CATEGORY_KEY to "not_a_category")),
                repository = repository,
                installedListings = FakeInstalledListings(),
            )
        }

        assertTrue(failure.message.orEmpty().contains("not_a_category"))
    }

    @Test
    fun `changing the sort refetches from offset zero within the same category`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        val model = viewModel(ListingCategory.Gaming)
        model.listings.asSnapshot()
        repository.browsedOffsets.clear()
        repository.browsedSorts.clear()
        repository.browsedCategories.clear()

        model.onSortChange(BrowseSortOption.NameAscending)
        model.listings.asSnapshot()

        assertEquals(listOf(0), repository.browsedOffsets)
        assertEquals(listOf("name-asc"), repository.browsedSorts)
        assertEquals(listOf(ListingCategory.Gaming), repository.browsedCategories)
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
    fun `a later page stays inside the category`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), true))

        viewModel(ListingCategory.Media).listings.asSnapshot()

        assertTrue(repository.browsedOffsets.contains(BROWSE_PAGE_SIZE))
        assertTrue(repository.browsedCategories.all { it == ListingCategory.Media })
    }
}
