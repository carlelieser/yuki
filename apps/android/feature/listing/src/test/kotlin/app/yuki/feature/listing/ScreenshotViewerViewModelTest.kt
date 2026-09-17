package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.Screenshot
import app.yuki.core.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private val SEEDED_URLS = listOf("https://example.test/one.png", "https://example.test/two.png")

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenshotViewerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModelWith(
        urls: List<String> = SEEDED_URLS,
        detail: Result<ListingDetail> = Result.success(detail()),
    ): ScreenshotViewerViewModel =
        ScreenshotViewerViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    LISTING_SLUG_KEY to SLUG,
                    SCREENSHOT_START_INDEX_KEY to 1,
                    SCREENSHOT_URLS_KEY to urls,
                ),
            ),
            repository = FakeListingRepository(detail),
        )

    @Test
    fun theViewerOpensOnTheRouteScreenshotsWithoutWaitingForTheNetwork() {
        val viewModel = viewModelWith()

        val state = viewModel.screenshots.value
        assertEquals(
            UiState.Success(SEEDED_URLS.map { url -> Screenshot(url = url, alt = null) }),
            state,
        )
    }

    @Test
    fun theViewerStillLoadsWhenTheRouteCarriesNoScreenshots() {
        val viewModel = viewModelWith(urls = emptyList())

        assertEquals(UiState.Loading, viewModel.screenshots.value)
    }

    @Test
    fun aFailedRefreshKeepsTheScreenshotsTheRouteCarried() = runTest(dispatcher) {
        val viewModel = viewModelWith(
            detail = Result.failure(TypedFailure(FailureReason.Offline)),
        )

        advanceUntilIdle()

        val state = viewModel.screenshots.value
        assertEquals(
            UiState.Success(SEEDED_URLS.map { url -> Screenshot(url = url, alt = null) }),
            state,
        )
    }

    @Test
    fun aSuccessfulRefreshReplacesTheSeedWithTheFetchedScreenshots() = runTest(dispatcher) {
        val viewModel = viewModelWith()

        advanceUntilIdle()

        assertEquals(UiState.Success(detail().screenshots), viewModel.screenshots.value)
    }

    @Test
    fun theStartIndexComesFromTheRoute() {
        assertEquals(1, viewModelWith().startIndex)
    }
}
