package app.yuki.feature.search

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.BACK_ACTION_TAG
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.designsystem.component.PULL_TO_REFRESH_TAG
import app.yuki.core.model.ListingSummary
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class AuthorScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val noCallbacks = AuthorCallbacks(
        sort = BrowseSortOption.Default,
        onSortSelected = {},
        onListingSelected = {},
        onBackClick = {},
    )

    private fun render(author: String = "acme") {
        composeRule.setContent {
            AuthorScreen(
                browsed = BrowsedAuthor(author = author, installs = ListingInstalls()),
                listings = emptyListings(),
                callbacks = noCallbacks,
                contentPadding = PaddingValues(),
            )
        }
    }

    @Test
    fun theBrowseListHostsThePullToRefreshGesture() {
        render()

        composeRule.onNodeWithTag(PULL_TO_REFRESH_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(BROWSE_LIST_TAG).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BROWSE_LIST_TAG).assertIsDisplayed()
    }

    @Test
    fun theAuthorNameTitlesTheScreen() {
        render("nullpointer")

        composeRule.onNodeWithText("nullpointer").assertIsDisplayed()
    }

    @Test
    fun theAuthorScreenKeepsItsBackButton() {
        render()

        composeRule.onNodeWithTag(BACK_ACTION_TAG).assertIsDisplayed()
    }

    @Test
    fun theAuthorScreenOffersSortWithoutTheCategoryChipRow() {
        render()

        composeRule.onNodeWithTag(SORT_SELECTOR_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CATEGORY_FILTER_TAG).assertDoesNotExist()
    }
}

@Composable
private fun emptyListings(): LazyPagingItems<ListingSummary> =
    flowOf(PagingData.empty<ListingSummary>()).collectAsLazyPagingItems()
