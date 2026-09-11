package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.LISTING_BADGE_ROW_TAG
import app.yuki.core.designsystem.component.ProductListItem
import app.yuki.core.designsystem.component.ProductListItemContent
import app.yuki.core.designsystem.component.toProductListItemContent
import app.yuki.core.designsystem.theme.YukiTheme
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductListItemTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun summary(
        category: ListingCategory? = ListingCategory.Gaming,
        ratingAverage: Double? = 4.6,
    ) = ListingSummary(
        id = "6f1d0f3a-0000-4000-8000-000000000001",
        githubRepoId = 1L,
        slug = "example-app",
        title = "Example App",
        author = "octocat",
        description = null,
        iconUrl = null,
        bannerUrl = null,
        category = category,
        stars = 1234,
        ratingAverage = ratingAverage,
        ratingCount = if (ratingAverage == null) 0 else 12,
    )

    private fun render(content: @Composable () -> ProductListItemContent) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                ProductListItem(content = content())
            }
        }
    }

    @Test
    fun aListingRowCarriesCategoryStarsRatingAndAuthor() {
        render { summary().toProductListItemContent() }

        composeRule.onNodeWithText("Gaming").assertIsDisplayed()
        composeRule.onNodeWithText("1.2K").assertIsDisplayed()
        composeRule.onNodeWithText("4.6").assertIsDisplayed()
        composeRule.onNodeWithText("octocat").assertIsDisplayed()
    }

    @Test
    fun anUnratedListingRowOmitsTheRatingBadge() {
        render { summary(ratingAverage = null).toProductListItemContent() }

        composeRule.onNodeWithText("1.2K").assertIsDisplayed()
        composeRule.onNodeWithText("4.6").assertDoesNotExist()
    }

    @Test
    fun anUncategorisedListingRowOmitsTheCategoryBadge() {
        render { summary(category = null).toProductListItemContent() }

        composeRule.onNodeWithText("Gaming").assertDoesNotExist()
        composeRule.onNodeWithText("octocat").assertIsDisplayed()
    }

    @Test
    fun aHandBuiltRowKeepsItsSupportingTextAndShowsNoBadges() {
        render {
            ProductListItemContent(
                title = "Example App",
                supporting = "v1.0.0 -> v1.1.0",
                iconUrl = null,
            )
        }

        composeRule.onNodeWithText("v1.0.0 -> v1.1.0").assertIsDisplayed()
        composeRule.onNodeWithTag(LISTING_BADGE_ROW_TAG).assertDoesNotExist()
    }
}
