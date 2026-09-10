package app.yuki.feature.search

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.BACK_ACTION_TAG
import app.yuki.core.designsystem.component.COLLECTION_EMPTY_TAG
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val noCallbacks = SearchCallbacks(
        onQueryChange = {},
        onRecentRemoved = {},
        onCategorySelected = {},
        onSortSelected = {},
        onListingSelected = {},
        onBackClick = {},
    )

    private fun browsing(
        category: ListingCategory? = null,
        sort: BrowseSortOption = BrowseSortOption.Default,
    ) = UiState.Success(
        SearchContent(
            query = "",
            recent = emptyList(),
            results = null,
            filter = BrowseFilter(sort = sort, category = category),
        ),
    )

    private fun searching(results: UiState<List<ListingSummary>>) = UiState.Success(
        SearchContent(
            query = "ghost",
            recent = emptyList(),
            results = results,
            filter = BrowseFilter(),
        ),
    )

    private fun render(state: UiState<SearchContent>) {
        composeRule.setContent {
            SearchScreen(
                state = state,
                listings = emptyListings(),
                callbacks = noCallbacks,
                contentPadding = PaddingValues(),
            )
        }
    }

    @Test
    fun theBrowseViewOffersTheFilterControlsAndABackButton() {
        render(browsing())

        composeRule.onNodeWithTag(BACK_ACTION_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CATEGORY_FILTER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SORT_SELECTOR_TAG).assertIsDisplayed()
    }

    @Test
    fun theSelectedSortLabelIsShown() {
        render(browsing(sort = BrowseSortOption.NameAscending))

        composeRule.onNodeWithText("Name A-Z").assertIsDisplayed()
    }

    @Test
    fun theAllOptionLeadsTheCategoryFilter() {
        render(browsing())

        composeRule.onNodeWithText(ALL_CATEGORIES_LABEL).assertIsDisplayed()
    }

    @Test
    fun searchResultsReplaceTheBrowseList() {
        render(searching(UiState.Success(listOf(listing("beta")))))

        composeRule.onNodeWithTag(SEARCH_RESULTS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Beta").assertIsDisplayed()
        composeRule.onNodeWithTag(CATEGORY_FILTER_TAG).assertDoesNotExist()
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
    category = null,
    stars = 1,
)
