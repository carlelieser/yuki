package app.yuki.feature.listing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.paging.PagingData
import androidx.test.platform.app.InstrumentationRegistry
import app.yuki.core.designsystem.component.DETAIL_HEADING_ICON_TAG
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.model.UiState
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class ReviewsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun headsTheScreenWithTheListingAndListsEveryReview() {
        val reviews = (1..12).map { index -> review("r$index") }

        composeRule.setContent {
            ReviewsScreen(
                content = ReviewsContent(
                    listing = ReviewedListing(title = "Aurora", iconUrl = null),
                    summary = UiState.Success(ratingSummary(total = 12)),
                    reviews = flowOf(PagingData.from(reviews)).collectAsLazyPagingItems(),
                ),
                onBackClick = {},
                onRefresh = {},
            )
        }

        composeRule.onNodeWithText("Aurora").assertIsDisplayed()
        composeRule.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.listing_section_reviews),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag(DETAIL_HEADING_ICON_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(RATING_SUMMARY_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(REVIEWS_LIST_TAG).performScrollToNode(hasText("Review r12"))
        composeRule.onNodeWithText("Review r12").assertIsDisplayed()
    }
}
