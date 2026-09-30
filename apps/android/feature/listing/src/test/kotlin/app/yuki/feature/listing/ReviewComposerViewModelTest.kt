package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import app.yuki.core.model.FailureReason
import app.yuki.core.model.MAX_REVIEW_BODY
import app.yuki.core.model.OwnReview
import app.yuki.core.model.ReviewDraft
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
class ReviewComposerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModelWith(repository: FakeReviewRepository): ReviewComposerViewModel {
        val viewModel = ReviewComposerViewModel(
            savedStateHandle = SavedStateHandle(mapOf(LISTING_SLUG_KEY to SLUG)),
            repository = repository,
        )
        dispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `starts empty when the viewer has not reviewed yet`() = runTest {
        val state = viewModelWith(FakeReviewRepository()).state.value

        assertFalse(state.isLoading)
        assertFalse(state.isEditing)
        assertEquals(0, state.rating)
        assertEquals("", state.body)
    }

    @Test
    fun `prefills the viewer's existing review for editing`() = runTest {
        val own = OwnReview(review = review("mine", rating = 2, body = "Crashes a lot"), canReview = true)
        val state = viewModelWith(FakeReviewRepository(ownResult = Result.success(own))).state.value

        assertTrue(state.isEditing)
        assertEquals(2, state.rating)
        assertEquals("Crashes a lot", state.body)
    }

    @Test
    fun `refuses to post without a rating`() = runTest {
        val repository = FakeReviewRepository()
        val viewModel = viewModelWith(repository)

        viewModel.onBodyChange("Great")
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isRatingMissing)
        assertEquals(emptyList<ReviewDraft>(), repository.saved)
    }

    @Test
    fun `posts the trimmed draft and finishes`() = runTest {
        val repository = FakeReviewRepository()
        val viewModel = viewModelWith(repository)

        viewModel.onRatingChange(5)
        viewModel.onBodyChange("  Great launcher  ")
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(ReviewDraft(rating = 5, body = "Great launcher")), repository.saved)
        assertTrue(viewModel.state.value.isDone)
    }

    @Test
    fun `keeps the body within the length limit`() = runTest {
        val viewModel = viewModelWith(FakeReviewRepository())

        viewModel.onBodyChange("a".repeat(MAX_REVIEW_BODY + 50))

        assertEquals(MAX_REVIEW_BODY, viewModel.state.value.body.length)
    }

    @Test
    fun `a refused post stays open with the server explanation`() = runTest {
        val refusal = FailureReason.Rejected("Download this app before reviewing it.")
        val repository = FakeReviewRepository().apply {
            saveResult = Result.failure(TypedFailure(refusal))
        }
        val viewModel = viewModelWith(repository)

        viewModel.onRatingChange(4)
        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(refusal, state.failure)
        assertFalse(state.isDone)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `deletes only after confirming`() = runTest {
        val own = OwnReview(review = review("mine"), canReview = true)
        val repository = FakeReviewRepository(ownResult = Result.success(own))
        val viewModel = viewModelWith(repository)

        viewModel.requestDelete()
        assertTrue(viewModel.state.value.isConfirmingDelete)
        viewModel.dismissDelete()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, repository.deleteCount)

        viewModel.requestDelete()
        viewModel.confirmDelete()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.deleteCount)
        assertTrue(viewModel.state.value.isDone)
    }
}
