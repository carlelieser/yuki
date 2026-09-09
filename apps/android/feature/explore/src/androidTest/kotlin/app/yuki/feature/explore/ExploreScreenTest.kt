package app.yuki.feature.explore

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.COLLECTION_EMPTY_TAG
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class ExploreScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val noCallbacks = ExploreCallbacks(
        onQueryChange = {},
        onRecentRemoved = {},
        onRetry = {},
        onListingSelected = {},
        onSettingsClick = {},
    )

    private fun searching(results: UiState<List<ListingSummary>>) = UiState.Success(
        ExploreContent(
            featured = UiState.Success(emptyList()),
            search = SearchState(query = "ghost", recent = emptyList(), results = results),
        ),
    )

    private fun render(state: UiState<ExploreContent>) {
        composeRule.setContent {
            ExploreScreen(
                state = state,
                listings = emptyListings(),
                callbacks = noCallbacks,
                contentPadding = PaddingValues(),
            )
        }
    }

    @Test
    fun anEmptySearchRendersTheEmptyStateNotAFailure() {
        render(searching(UiState.Success(emptyList())))

        composeRule.onNodeWithTag(COLLECTION_EMPTY_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertDoesNotExist()
    }

    @Test
    fun aFailedSearchRendersTheFailureStateNotTheEmptyState() {
        render(searching(UiState.Failure(FailureReason.Offline)))

        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_EMPTY_TAG).assertDoesNotExist()
    }

    @Test
    fun searchResultsReplaceTheBrowseList() {
        render(searching(UiState.Success(listOf(listing("beta")))))

        composeRule.onNodeWithTag(SEARCH_RESULTS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Beta").assertIsDisplayed()
        composeRule.onNodeWithTag(FEATURED_ROW_TAG).assertDoesNotExist()
    }

    @Test
    fun featuredListingsRenderWhenNotSearching() {
        render(
            UiState.Success(
                ExploreContent(
                    featured = UiState.Success(listOf(listing("alpha"))),
                    search = SearchState(query = "", recent = emptyList(), results = null),
                ),
            ),
        )

        composeRule.onNodeWithTag(FEATURED_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SEARCH_RESULTS_TAG).assertDoesNotExist()
    }

    @Test
    fun aScreenFailureOffersRetry() {
        render(UiState.Failure(FailureReason.Server(500)))

        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertIsDisplayed()
    }
}

@Composable
private fun emptyListings(): LazyPagingItems<ListingSummary> =
    flowOf(PagingData.empty<ListingSummary>()).collectAsLazyPagingItems()

private fun listing(slug: String) = ListingSummary(
    id = slug,
    githubRepoId = slug.hashCode().toLong(),
    slug = slug,
    title = slug.replaceFirstChar(Char::uppercase),
    author = "yuki",
    description = null,
    iconUrl = null,
    bannerUrl = null,
    stars = 1,
)
