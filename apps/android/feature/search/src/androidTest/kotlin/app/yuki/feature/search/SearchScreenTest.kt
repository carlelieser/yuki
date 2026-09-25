package app.yuki.feature.search

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.R as DesignR
import app.yuki.core.designsystem.component.BACK_ACTION_TAG
import app.yuki.core.designsystem.component.COLLECTION_EMPTY_TAG
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.designsystem.component.PULL_TO_REFRESH_TAG
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
    )

    private fun browsingWithRecent(recent: List<String>) = UiState.Success(
        SearchContent(
            query = "",
            recent = recent,
            results = null,
            filter = BrowseFilter(),
        ),
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
    fun theBrowseListHostsThePullToRefreshGesture() {
        render(browsing())

        composeRule.onNodeWithTag(PULL_TO_REFRESH_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(BROWSE_LIST_TAG).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BROWSE_LIST_TAG).assertIsDisplayed()
    }

    @Test
    fun theSearchResultsListDoesNotOfferPullToRefresh() {
        render(searching(results = UiState.Success(listOf(listing("alpha")))))

        composeRule.onNodeWithTag(PULL_TO_REFRESH_TAG).assertDoesNotExist()
    }

    @Test
    fun theBrowseTabOffersTheFilterControlsWithoutABackButton() {
        render(browsing())

        composeRule.onNodeWithTag(CATEGORY_FILTER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SORT_SELECTOR_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(BACK_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun theSortControlSitsInTheScreenHeaderAndNamesTheActiveSort() {
        render(browsing(sort = BrowseSortOption.NameAscending))

        composeRule.onNodeWithContentDescription("$SORT_DESCRIPTION Name A-Z")
            .assertIsDisplayed()
    }

    @Test
    fun theSortControlStaysAvailableWhileSearchResultsShow() {
        render(searching(UiState.Success(listOf(listing("beta")))))

        composeRule.onNodeWithTag(SORT_SELECTOR_TAG).assertIsDisplayed()
    }

    @Test
    fun recentSearchesStayHiddenUntilTheSearchBarIsFocused() {
        render(browsingWithRecent(listOf("beta")))

        composeRule.onNodeWithTag(RECENT_SEARCHES_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(CATEGORY_FILTER_TAG).assertIsDisplayed()
    }

    @Test
    fun focusingTheSearchBarRevealsRecentSearchesAndHidesTheBrowseList() {
        render(browsingWithRecent(listOf("beta")))

        composeRule.onNodeWithText(
            composeRule.activity.getString(DesignR.string.designsystem_search_placeholder),
        ).performClick()

        composeRule.onNodeWithTag(RECENT_SEARCHES_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(RECENT_SEARCHES_TITLE.uppercase()).assertIsDisplayed()
        composeRule.onNodeWithTag(CATEGORY_FILTER_TAG).assertDoesNotExist()
    }

    @Test
    fun aQueryKeepsSearchModeOpenWithoutFocus() {
        render(searching(UiState.Success(listOf(listing("beta")))))

        composeRule.onNodeWithTag(SEARCH_RESULTS_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CATEGORY_FILTER_TAG).assertDoesNotExist()
    }

    @Test
    fun everySortOptionIsReachableFromTheDropdown() {
        render(browsing())

        composeRule.onNodeWithTag(SORT_SELECTOR_TAG).performClick()

        BrowseSortOption.entries.forEach { option ->
            composeRule.onNodeWithText(option.label).assertIsDisplayed()
        }
    }

    @Test
    fun theDividerStaysHiddenUntilTheListScrolls() {
        render(browsing())

        composeRule.onNodeWithTag(BROWSE_DIVIDER_TAG).assertDoesNotExist()
    }

    @Test
    fun theFilterOffersEveryCategoryAndNoAllOption() {
        render(browsing())

        composeRule.onNodeWithText("All").assertDoesNotExist()
        composeRule.onNodeWithText(ListingCategory.Media.label).assertIsDisplayed()
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
