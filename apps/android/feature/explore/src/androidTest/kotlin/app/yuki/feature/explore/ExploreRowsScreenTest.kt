package app.yuki.feature.explore

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.yuki.core.designsystem.R as DesignR
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExploreRowsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val seeAllSorts = mutableListOf<String>()

    private fun render() {
        composeRule.setContent {
            ExploreScreen(
                state = UiState.Success(
                    ExploreContent(
                        featured = UiState.Success(emptyList()),
                        sections = UiState.Success(emptyList()),
                        newest = UiState.Success(listOf(listing("fresh"))),
                        updated = UiState.Success(listOf(listing("patched"))),
                    ),
                ),
                refresh = ExploreRefresh(isRefreshing = false, onPullToRefresh = {}),
                callbacks = ExploreCallbacks(
                    onRetry = {},
                    onListingSelected = {},
                    onCategorySelected = {},
                    onSeeAllSelected = { sort -> seeAllSorts += sort },
                ),
                contentPadding = PaddingValues(),
            )
        }
    }

    private fun seeAll(titleRes: Int) {
        val title = composeRule.activity.getString(titleRes)
        composeRule
            .onNodeWithContentDescription(
                composeRule.activity.getString(DesignR.string.designsystem_section_see_all, title),
            )
            .performClick()
    }

    @Test
    fun newestAndRecentlyUpdatedRowsRenderTheirListings() {
        render()

        composeRule.onNodeWithText("Fresh").assertIsDisplayed()
        composeRule.onNodeWithText("Patched").assertIsDisplayed()
    }

    @Test
    fun seeAllOpensEachRowInItsOwnOrder() {
        render()

        seeAll(R.string.explore_new_title)
        seeAll(R.string.explore_updated_title)

        assertEquals(listOf("newest", "updated"), seeAllSorts)
    }
}

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
