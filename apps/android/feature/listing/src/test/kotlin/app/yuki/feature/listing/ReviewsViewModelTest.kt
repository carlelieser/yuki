package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewsViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `identifies the listing the reviews belong to`() {
        val viewModel = ReviewsViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    LISTING_SLUG_KEY to SLUG,
                    REVIEWED_TITLE_KEY to "Aurora",
                    REVIEWED_ICON_KEY to "https://cdn.test/icon.png",
                ),
            ),
            repository = FakeReviewRepository(),
        )

        assertEquals(ReviewedListing(title = "Aurora", iconUrl = "https://cdn.test/icon.png"), viewModel.listing)
    }
}
