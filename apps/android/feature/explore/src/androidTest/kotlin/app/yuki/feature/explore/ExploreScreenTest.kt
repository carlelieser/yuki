package app.yuki.feature.explore

import androidx.compose.ui.test.assertIsDisplayed
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import app.yuki.core.designsystem.component.COLLECTION_EMPTY_TAG
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.model.CategorySection
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExploreScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val chosenCategories = mutableListOf<ListingCategory>()

    private val noCallbacks = ExploreCallbacks(
        onQueryChange = {},
        onRecentRemoved = {},
        onRetry = {},
        onListingSelected = {},
        onCategorySelected = { category -> chosenCategories += category },
        onSettingsClick = {},
    )

    private fun searching(results: UiState<List<ListingSummary>>) = UiState.Success(
        ExploreContent(
            featured = UiState.Success(emptyList()),
            sections = UiState.Success(emptyList()),
            search = SearchState(query = "ghost", recent = emptyList(), results = results),
        ),
    )

    private fun browsing(
        featured: UiState<List<ListingSummary>> = UiState.Success(emptyList()),
        sections: UiState<List<CategorySection>> = UiState.Success(emptyList()),
    ) = UiState.Success(
        ExploreContent(
            featured = featured,
            sections = sections,
            search = SearchState(query = "", recent = emptyList(), results = null),
        ),
    )

    private fun render(state: UiState<ExploreContent>) {
        composeRule.setContent {
            ExploreScreen(
                state = state,
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
        render(browsing(featured = UiState.Success(listOf(listing("alpha")))))

        composeRule.onNodeWithTag(FEATURED_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SEARCH_RESULTS_TAG).assertDoesNotExist()
    }

    @Test
    fun eachCategorySectionRendersItsLabelAndListings() {
        render(
            browsing(
                sections = UiState.Success(
                    listOf(
                        section(ListingCategory.Gaming, listOf("alpha")),
                        section(ListingCategory.Media, listOf("beta")),
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText(ListingCategory.Gaming.label).assertIsDisplayed()
        composeRule.onNodeWithText(ListingCategory.Media.label).assertIsDisplayed()
        composeRule.onNodeWithText("Alpha").assertIsDisplayed()
    }

    @Test
    fun theSectionArrowReportsItsOwnCategory() {
        render(
            browsing(
                sections = UiState.Success(
                    listOf(section(ListingCategory.Gaming, listOf("alpha"))),
                ),
            ),
        )

        composeRule
            .onNodeWithContentDescription(
                "$CATEGORY_SECTION_ARROW_DESCRIPTION ${ListingCategory.Gaming.label}",
            )
            .performClick()

        assertEquals(listOf(ListingCategory.Gaming), chosenCategories)
    }

    @Test
    fun aSectionsFailureKeepsFeaturedOnScreen() {
        render(
            browsing(
                featured = UiState.Success(listOf(listing("alpha"))),
                sections = UiState.Failure(FailureReason.Offline),
            ),
        )

        composeRule.onNodeWithTag(FEATURED_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertIsDisplayed()
    }

    @Test
    fun anEmptySectionsListRendersTheEmptyState() {
        render(browsing(sections = UiState.Success(emptyList())))

        composeRule.onNodeWithTag(COLLECTION_EMPTY_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertDoesNotExist()
    }

    @Test
    fun aScreenFailureOffersRetry() {
        render(UiState.Failure(FailureReason.Server(500)))

        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertIsDisplayed()
    }
}

private fun section(
    category: ListingCategory,
    slugs: List<String>,
): CategorySection = CategorySection(category = category, results = slugs.map(::listing))

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
