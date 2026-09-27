package app.yuki.feature.search

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.model.ListingSummary
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class CatalogScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render(list: CatalogList) {
        composeRule.setContent {
            CatalogScreen(
                browsed = BrowsedCatalog(list = list, installs = ListingInstalls()),
                listings = flowOf(PagingData.empty<ListingSummary>()).collectAsLazyPagingItems(),
                callbacks = CatalogCallbacks(onListingSelected = {}, onBackClick = {}),
                contentPadding = PaddingValues(),
            )
        }
    }

    @Test
    fun theNewListIsTitledNew() {
        render(CatalogList.Newest)

        composeRule.onNodeWithText("New").assertIsDisplayed()
    }

    @Test
    fun theUpdatedListIsTitledRecentlyUpdated() {
        render(CatalogList.RecentlyUpdated)

        composeRule.onNodeWithText("Recently updated").assertIsDisplayed()
    }

    @Test
    fun theListOffersNoSortMenuThatWouldContradictItsTitle() {
        render(CatalogList.Newest)

        composeRule.onAllNodesWithTag(SORT_SELECTOR_TAG).assertCountEquals(0)
    }
}
