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
import app.yuki.core.designsystem.component.PULL_TO_REFRESH_TAG
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class CategoryScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val noCallbacks = CategoryCallbacks(
        sort = BrowseSortOption.Default,
        onSortSelected = {},
        onListingSelected = {},
        onBackClick = {},
    )

    private fun render(category: ListingCategory = ListingCategory.Gaming) {
        composeRule.setContent {
            CategoryScreen(
                browsed = BrowsedCategory(category = category, installedIds = emptySet()),
                listings = emptyListings(),
                callbacks = noCallbacks,
                contentPadding = PaddingValues(),
            )
        }
    }

    @Test
    fun theBrowseListHostsThePullToRefreshGesture() {
        render(ListingCategory.Gaming)

        composeRule.onNodeWithTag(PULL_TO_REFRESH_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(BROWSE_LIST_TAG).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(BROWSE_LIST_TAG).assertIsDisplayed()
    }

    @Test
    fun theCategoryNameTitlesTheScreen() {
        render(ListingCategory.Gaming)

        composeRule.onNodeWithText(ListingCategory.Gaming.label).assertIsDisplayed()
    }

    @Test
    fun theCategoryScreenKeepsItsBackButton() {
        render()

        composeRule.onNodeWithTag(BACK_ACTION_TAG).assertIsDisplayed()
    }

    @Test
    fun theCategoryScreenOffersSortWithoutTheCategoryChipRow() {
        render()

        composeRule.onNodeWithTag(SORT_SELECTOR_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CATEGORY_FILTER_TAG).assertDoesNotExist()
    }
}

@Composable
private fun emptyListings(): LazyPagingItems<ListingSummary> =
    flowOf(PagingData.empty<ListingSummary>()).collectAsLazyPagingItems()
