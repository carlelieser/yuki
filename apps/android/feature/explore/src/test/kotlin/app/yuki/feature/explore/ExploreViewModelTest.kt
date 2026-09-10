package app.yuki.feature.explore

import app.cash.turbine.ReceiveTurbine
import app.yuki.core.model.CategorySection
import app.cash.turbine.test
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ExploreViewModelTest {
    private val repository = FakeListingRepository()
    
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ExploreViewModel(repository)

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
    fun `emits loading then failure when every source fails`() = runTest {
        repository.featuredResult = Result.failure(TypedFailure(FailureReason.Offline))
        repository.sectionsResult = Result.failure(TypedFailure(FailureReason.Offline))

        viewModel().state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Failure(FailureReason.Offline), awaitTerminal())
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
    fun `sections load alongside featured and keep the server order`() = runTest {
        repository.featuredResult = Result.success(listOf(listing("alpha")))
        repository.sectionsResult = Result.success(
            listOf(
                section(ListingCategory.Gaming, listOf("one", "two", "three")),
                section(ListingCategory.Media, listOf("four")),
            ),
        )

        viewModel().state.test {
            val loaded = awaitLoadedSections()

            assertEquals(
                listOf(ListingCategory.Gaming, ListingCategory.Media),
                loaded.map { entry -> entry.category },
            )
            assertEquals(3, loaded.first().results.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `asks the server for three listings per section`() = runTest {
        repository.featuredResult = Result.success(listOf(listing("alpha")))

        viewModel().state.test {
            awaitLoadedSections()

            assertEquals(listOf(SECTION_ITEM_COUNT), repository.sectionLimits)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a sections failure does not blank a screen whose featured loaded`() = runTest {
        repository.featuredResult = Result.success(listOf(listing("alpha")))
        repository.sectionsResult = Result.failure(TypedFailure(FailureReason.Offline))

        viewModel().state.test {
            val loaded = awaitSettledContent()

            assertTrue(loaded.featured is UiState.Success)
            assertEquals(UiState.Failure(FailureReason.Offline), loaded.sections)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a featured failure does not blank a screen whose sections loaded`() = runTest {
        repository.featuredResult = Result.failure(TypedFailure(FailureReason.Offline))
        repository.sectionsResult = Result.success(
            listOf(section(ListingCategory.Gaming, listOf("one"))),
        )

        viewModel().state.test {
            val loaded = awaitSettledContent()

            assertEquals(UiState.Failure(FailureReason.Offline), loaded.featured)
            assertTrue(loaded.sections is UiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the screen fails only when featured and sections both fail`() = runTest {
        repository.featuredResult = Result.failure(TypedFailure(FailureReason.Offline))
        repository.sectionsResult = Result.failure(TypedFailure(FailureReason.Offline))

        viewModel().state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Failure(FailureReason.Offline), awaitTerminal())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an empty sections list is a success not a failure`() = runTest {
        repository.featuredResult = Result.success(listOf(listing("alpha")))
        repository.sectionsResult = Result.success(emptyList())

        viewModel().state.test {
            val loaded = awaitSettledContent()

            val sections = loaded.sections as UiState.Success
            assertTrue(sections.data.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reloads both featured and sections`() = runTest {
        repository.featuredResult = Result.failure(TypedFailure(FailureReason.Offline))
        repository.sectionsResult = Result.failure(TypedFailure(FailureReason.Offline))

        val model = viewModel()
        model.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Failure(FailureReason.Offline), awaitTerminal())

            repository.featuredResult = Result.success(listOf(listing("alpha")))
            repository.sectionsResult = Result.success(
                listOf(section(ListingCategory.Gaming, listOf("one"))),
            )
            model.refresh()

            val loaded = awaitSettledContent()
            assertTrue(loaded.featured is UiState.Success)
            assertTrue(loaded.sections is UiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retrying featured recovers from a failure`() = runTest {
        repository.featuredResult = Result.failure(TypedFailure(FailureReason.Offline))
        repository.sectionsResult = Result.failure(TypedFailure(FailureReason.Offline))

        val model = viewModel()
        model.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Failure(FailureReason.Offline), awaitTerminal())

            repository.featuredResult = Result.success(listOf(listing("alpha")))
            model.refreshFeatured()

            val loaded = awaitLoadedFeatured()
            assertEquals(1, loaded.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private suspend fun ReceiveTurbine<UiState<ExploreContent>>.awaitLoadedFeatured():
    List<ListingSummary> {
    while (true) {
        val content = (awaitItem() as? UiState.Success)?.data ?: continue
        val featured = content.featured
        if (featured is UiState.Success) return featured.data
    }
}

private suspend fun ReceiveTurbine<UiState<ExploreContent>>.awaitTerminal():
    UiState<ExploreContent> {
    while (true) {
        val state = awaitItem()
        if (state !is UiState.Loading) return state
    }
}

private suspend fun ReceiveTurbine<UiState<ExploreContent>>.awaitSettledContent():
    ExploreContent {
    while (true) {
        val content = (awaitItem() as? UiState.Success)?.data ?: continue
        val isSettled = content.featured !is UiState.Loading && content.sections !is UiState.Loading
        if (isSettled) return content
    }
}

private suspend fun ReceiveTurbine<UiState<ExploreContent>>.awaitLoadedSections():
    List<CategorySection> {
    while (true) {
        val content = (awaitItem() as? UiState.Success)?.data ?: continue
        val sections = content.sections
        if (sections is UiState.Success) return sections.data
    }
}


