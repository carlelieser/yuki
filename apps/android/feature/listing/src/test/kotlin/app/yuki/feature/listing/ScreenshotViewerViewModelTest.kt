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
        urls: Any? = SEEDED_URLS.toTypedArray(),
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

    private fun seededScreenshots(): UiState<List<Screenshot>> =
        UiState.Success(SEEDED_URLS.map { url -> Screenshot(url = url, alt = null) })

    @Test
    fun theViewerOpensOnTheRouteScreenshotsWithoutWaitingForTheNetwork() {
        val viewModel = viewModelWith()

        assertEquals(seededScreenshots(), viewModel.screenshots.value)
    }

    @Test
    fun theViewerStillLoadsWhenTheRouteCarriesNoScreenshots() {
        val viewModel = viewModelWith(urls = emptyArray<String>())

        assertEquals(UiState.Loading, viewModel.screenshots.value)
    }

    @Test
    fun theViewerOpensWhenTheRouteStoredItsUrlsAsAList() {
        val viewModel = viewModelWith(urls = SEEDED_URLS)

        assertEquals(seededScreenshots(), viewModel.screenshots.value)
    }

    @Test
    fun theViewerStillLoadsWhenTheRouteCarriesNothingAtAll() {
        val viewModel = viewModelWith(urls = null)

        assertEquals(UiState.Loading, viewModel.screenshots.value)
    }

    @Test
    fun aFailedRefreshKeepsTheScreenshotsTheRouteCarried() = runTest(dispatcher) {
        val viewModel = viewModelWith(
            detail = Result.failure(TypedFailure(FailureReason.Offline)),
        )

        advanceUntilIdle()

        assertEquals(seededScreenshots(), viewModel.screenshots.value)
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
