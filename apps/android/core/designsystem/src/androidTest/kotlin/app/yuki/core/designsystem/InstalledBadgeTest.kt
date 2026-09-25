package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.INSTALLED_BADGE_TAG
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
class InstalledBadgeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(@StringRes id: Int, vararg args: Any): String =
        composeRule.activity.getString(id, *args)

    private fun summary() = ListingSummary(
        id = "6f1d0f3a-0000-4000-8000-000000000001",
        githubRepoId = 1L,
        slug = "example-app",
        title = "Example App",
        author = "octocat",
        description = null,
        iconUrl = null,
        bannerUrl = null,
        category = ListingCategory.Gaming,
        stars = 1234,
        ratingAverage = 4.6,
        ratingCount = 12,
    )

    private fun render(content: @Composable () -> ProductListItemContent) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                ProductListItem(content = content())
            }
        }
    }

    @Test
    fun anInstalledRowShowsTheInstalledBadge() {
        render { summary().toProductListItemContent().copy(isInstalled = true) }

        composeRule.onNodeWithTag(INSTALLED_BADGE_TAG).assertIsDisplayed()
    }

    @Test
    fun aRowThatIsNotInstalledOmitsTheInstalledBadge() {
        render { summary().toProductListItemContent() }

        composeRule.onNodeWithTag(INSTALLED_BADGE_TAG).assertDoesNotExist()
    }

    @Test
    fun anInstalledRowKeepsItsSupportingText() {
        render {
            ProductListItemContent(
                title = "Example App",
                supporting = "v1.0.0 -> v1.1.0",
                iconUrl = null,
                isInstalled = true,
            )
        }

        composeRule.onNodeWithText("v1.0.0 -> v1.1.0").assertIsDisplayed()
        composeRule.onNodeWithTag(INSTALLED_BADGE_TAG).assertIsDisplayed()
    }

    @Test
    fun anInstalledListingRowKeepsItsListingBadges() {
        render { summary().toProductListItemContent().copy(isInstalled = true) }

        composeRule.onNodeWithTag(LISTING_BADGE_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.designsystem_category_gaming)).assertIsDisplayed()
        composeRule.onNodeWithText("octocat").assertIsDisplayed()
        composeRule.onNodeWithTag(INSTALLED_BADGE_TAG).assertIsDisplayed()
    }

    @Test
    fun theInstalledBadgeReadsAsOneNodeToScreenReaders() {
        render { summary().toProductListItemContent().copy(isInstalled = true) }

        composeRule.onNodeWithContentDescription(text(R.string.designsystem_installed)).assertIsDisplayed()
    }
}
