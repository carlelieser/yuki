package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.yuki.core.model.FailureReason
import app.yuki.core.model.OwnReview
import app.yuki.core.model.ReviewPage
import app.yuki.core.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListingReviewsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModelWith(
        repository: FakeReviewRepository,
        sessions: FakeSessionStore = FakeSessionStore(),
    ): ListingReviewsViewModel = ListingReviewsViewModel(
        savedStateHandle = SavedStateHandle(mapOf(LISTING_SLUG_KEY to SLUG)),
        repository = repository,
        sessionStore = sessions,
    )

    private fun loaded(viewModel: ListingReviewsViewModel): ListingReviews =
        (viewModel.reviews.value as UiState.Success).data

    @Test
    fun `previews the three newest reviews with the summary`() = runTest {
        val newest = (1..5).map { index -> review("r$index") }
        val repository = FakeReviewRepository(
            summaryResult = Result.success(ratingSummary(total = 5)),
            pages = mapOf(0 to Result.success(ReviewPage(newest, hasMore = false))),
        )
        val viewModel = viewModelWith(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val reviews = loaded(viewModel)
        assertEquals(newest.take(3), reviews.preview)
        assertEquals(5, reviews.summary.total)
        assertTrue(reviews.hasMore)
    }

    @Test
    fun `a short list offers no see all`() = runTest {
        val repository = FakeReviewRepository(
            pages = mapOf(0 to Result.success(ReviewPage(listOf(review("r1")), hasMore = false))),
        )
        val viewModel = viewModelWith(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(loaded(viewModel).hasMore)
    }

    @Test
    fun `a failed summary surfaces as a failure and retries`() = runTest {
        val repository = FakeReviewRepository(summaryResult = Result.failure(TypedFailure(FailureReason.Offline)))
        val viewModel = viewModelWith(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(UiState.Failure(FailureReason.Offline), viewModel.reviews.value)

        repository.summaryResult = Result.success(ratingSummary())
        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.reviews.value is UiState.Success)
    }

    @Test
    fun `a signed-out viewer never asks for their review`() = runTest {
        val repository = FakeReviewRepository()
        val viewModel = viewModelWith(repository)

        viewModel.viewer.test {
            assertEquals(ReviewViewer(isSignedIn = false), awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            expectNoEvents()
        }
        assertEquals(0, repository.ownCallCount)
    }

    @Test
    fun `signing in loads the viewer's own review`() = runTest {
        val own = OwnReview(review = review("mine"), canReview = true)
        val repository = FakeReviewRepository(ownResult = Result.success(own))
        val sessions = FakeSessionStore()
        val viewModel = viewModelWith(repository, sessions)

        viewModel.viewer.test {
            assertEquals(ReviewViewer(isSignedIn = false), awaitItem())

            sessions.store(SIGNED_IN)
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(ReviewViewer(isSignedIn = true, own = own), awaitItem())
        }
    }

    @Test
    fun `a saved review reloads the section and the viewer's review`() = runTest {
        val repository = FakeReviewRepository()
        val viewModel = viewModelWith(repository, FakeSessionStore(SIGNED_IN))

        viewModel.viewer.test {
            dispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        val ownBefore = repository.ownCallCount
        val summaryBefore = repository.summaryCallCount

        viewModel.viewer.test {
            dispatcher.scheduler.advanceUntilIdle()
            repository.announce(SLUG)
            dispatcher.scheduler.advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(summaryBefore + 1, repository.summaryCallCount)
        assertTrue(repository.ownCallCount > ownBefore)
    }

    @Test
    fun `a change to another listing is ignored`() = runTest {
        val repository = FakeReviewRepository()
        viewModelWith(repository)
        dispatcher.scheduler.advanceUntilIdle()

        repository.announce("borealis")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.summaryCallCount)
    }
}
