package app.yuki.feature.listing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.model.UiState
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class ReviewsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listsTheSummaryAndEveryLoadedReview() {
        val reviews = (1..12).map { index -> review("r$index") }

        composeRule.setContent {
            ReviewsScreen(
                content = ReviewsContent(
                    summary = UiState.Success(ratingSummary(total = 12)),
                    reviews = flowOf(PagingData.from(reviews)).collectAsLazyPagingItems(),
                ),
                onBackClick = {},
                onRefresh = {},
            )
        }

        composeRule.onNodeWithTag(RATING_SUMMARY_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(REVIEWS_LIST_TAG).performScrollToNode(hasText("Review r12"))
        composeRule.onNodeWithText("Review r12").assertIsDisplayed()
    }
}
