package app.yuki.feature.explore

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import app.yuki.core.designsystem.R as DesignR
import app.yuki.core.designsystem.component.COLLECTION_EMPTY_TAG
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.designsystem.component.INSTALLED_BADGE_TAG
import app.yuki.core.designsystem.component.PAGE_INDICATOR_TAG
import app.yuki.core.designsystem.component.PULL_TO_REFRESH_TAG
import app.yuki.core.designsystem.theme.LocalReduceMotion
import app.yuki.core.designsystem.theme.LocalTouchExploration
import app.yuki.core.model.CategorySection
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExploreScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val chosenCategories = mutableListOf<ListingCategory>()

    private val noCallbacks = ExploreCallbacks(
        onRetry = {},
        onListingSelected = {},
        onCategorySelected = { category -> chosenCategories += category },
    )

    private fun browsing(
        featured: UiState<List<ListingSummary>> = UiState.Success(emptyList()),
        sections: UiState<List<CategorySection>> = UiState.Success(emptyList()),
        installedIds: Set<Long> = emptySet(),
    ) = UiState.Success(
        ExploreContent(
            featured = featured,
            sections = sections,
            installedIds = installedIds,
        ),
    )

    private fun render(
        state: UiState<ExploreContent>,
        onPullToRefresh: () -> Unit = {},
        isReduceMotion: Boolean = true,
        isTouchExploration: Boolean = false,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalReduceMotion provides isReduceMotion,
                LocalTouchExploration provides isTouchExploration,
            ) {
                ExploreScreen(
                    state = state,
                    refresh = ExploreRefresh(
                        isRefreshing = false,
                        onPullToRefresh = onPullToRefresh,
                    ),
                    callbacks = noCallbacks,
                    contentPadding = PaddingValues(),
                )
            }
        }
    }

    @Test
    fun featuredListingsRender() {
        render(browsing(featured = UiState.Success(listOf(listing("alpha")))))

        composeRule.onNodeWithTag(FEATURED_ROW_TAG).assertIsDisplayed()
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
    fun anInstalledListingIsMarkedInItsCategorySection() {
        val alpha = listing("alpha")

        render(
            browsing(
                sections = UiState.Success(
                    listOf(section(ListingCategory.Gaming, listOf("alpha", "beta"))),
                ),
                installedIds = setOf(alpha.githubRepoId),
            ),
        )

        composeRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeRule.onAllNodesWithTag(INSTALLED_BADGE_TAG).assertCountEquals(1)
    }

    @Test
    fun anInstalledListingIsMarkedInTheFeaturedCarousel() {
        val alpha = listing("alpha")

        render(
            browsing(
                featured = UiState.Success(listOf(alpha)),
                installedIds = setOf(alpha.githubRepoId),
            ),
        )

        composeRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeRule.onAllNodesWithTag(INSTALLED_BADGE_TAG).assertCountEquals(1)
    }

    @Test
    fun aFeaturedListingIsUnmarkedWhenItIsNotInstalled() {
        render(browsing(featured = UiState.Success(listOf(listing("alpha")))))

        composeRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeRule.onAllNodesWithTag(INSTALLED_BADGE_TAG).assertCountEquals(0)
    }

    @Test
    fun noListingIsMarkedWhenNothingIsInstalled() {
        render(
            browsing(
                sections = UiState.Success(
                    listOf(section(ListingCategory.Gaming, listOf("alpha", "beta"))),
                ),
            ),
        )

        composeRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeRule.onAllNodesWithTag(INSTALLED_BADGE_TAG).assertCountEquals(0)
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
                composeRule.activity.getString(
                    DesignR.string.designsystem_section_see_all,
                    ListingCategory.Gaming.label,
                ),
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

    @Test
    fun loadingFeaturedReservesTheSlotWithAPlaceholder() {
        render(
            browsing(
                featured = UiState.Loading,
                sections = UiState.Success(
                    listOf(section(ListingCategory.SystemTweaks, listOf("alpha"))),
                ),
            ),
        )

        composeRule.onNodeWithText(FEATURED_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(FEATURED_ROW_PLACEHOLDER_TAG).assertIsDisplayed()
    }

    @Test
    fun aFailedFeaturedDropsTheSlotEntirely() {
        render(
            browsing(
                featured = UiState.Failure(FailureReason.Offline),
                sections = UiState.Success(
                    listOf(section(ListingCategory.SystemTweaks, listOf("alpha"))),
                ),
            ),
        )

        composeRule.onNodeWithText(FEATURED_TITLE).assertDoesNotExist()
        composeRule.onNodeWithTag(FEATURED_ROW_PLACEHOLDER_TAG).assertDoesNotExist()
    }

    @Test
    fun pullingDownTheSectionsRequestsARefresh() {
        var refreshes = 0
        render(
            browsing(sections = UiState.Success(listOf(section(ListingCategory.Gaming, listOf("alpha"))))),
            onPullToRefresh = { refreshes += 1 },
        )

        composeRule.onNodeWithTag(CATEGORY_SECTIONS_TAG).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertEquals(1, refreshes)
    }

    @Test
    fun pullingDownTheFailureStateRequestsARefresh() {
        var refreshes = 0
        render(UiState.Failure(FailureReason.Offline), onPullToRefresh = { refreshes += 1 })

        composeRule.onNodeWithTag(PULL_TO_REFRESH_TAG).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertEquals(1, refreshes)
    }

    @Test
    fun theCarouselShowsAPageIndicator() {
        render(
            browsing(
                featured = UiState.Success(
                    listOf(listing("alpha"), listing("beta"), listing("gamma")),
                ),
            ),
        )

        composeRule.onNodeWithTag(PAGE_INDICATOR_TAG).assertIsDisplayed()
    }

    @Test
    fun aSingleFeaturedListingHasNoIndicator() {
        render(browsing(featured = UiState.Success(listOf(listing("alpha")))))

        composeRule.onNodeWithTag(PAGE_INDICATOR_TAG).assertDoesNotExist()
    }

    @Test
    fun aFeaturedCardAnnouncesItsPosition() {
        render(
            browsing(
                featured = UiState.Success(
                    listOf(listing("alpha"), listing("beta"), listing("gamma")),
                ),
            ),
        )

        composeRule
            .onNodeWithContentDescription(describeFeaturedListing("Alpha", 0, 3))
            .assertIsDisplayed()
    }

    @Test
    fun theCarouselOffersNextAndPreviousActions() {
        render(
            browsing(
                featured = UiState.Success(
                    listOf(listing("alpha"), listing("beta"), listing("gamma")),
                ),
            ),
        )

        val actions = composeRule
            .onNodeWithTag(FEATURED_ROW_TAG)
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .map { action -> action.label }

        assertEquals(listOf(NEXT_PAGE_LABEL, PREVIOUS_PAGE_LABEL), actions)
    }

    @Test
    fun theFirstSectionKeepsItsPositionWhenFeaturedResolves() {
        val sections = UiState.Success(
            listOf(section(ListingCategory.SystemTweaks, listOf("alpha"))),
        )
        val featured = mutableStateOf<UiState<List<ListingSummary>>>(UiState.Loading)

        composeRule.setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                ExploreScreen(
                    state = browsing(featured = featured.value, sections = sections),
                    refresh = ExploreRefresh(isRefreshing = false, onPullToRefresh = {}),
                    callbacks = noCallbacks,
                    contentPadding = PaddingValues(),
                )
            }
        }

        val whileLoading = composeRule
            .onNodeWithText(ListingCategory.SystemTweaks.label)
            .getUnclippedBoundsInRoot()
            .top

        composeRule.runOnIdle { featured.value = UiState.Success(listOf(listing("alpha"))) }
        composeRule.waitForIdle()

        val afterResolving = composeRule
            .onNodeWithText(ListingCategory.SystemTweaks.label)
            .getUnclippedBoundsInRoot()
            .top

        afterResolving.assertIsEqualTo(whileLoading, "first section top")
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
