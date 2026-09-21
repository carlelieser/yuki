package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.ListingBadgeRow
import app.yuki.core.designsystem.component.toBadges
import app.yuki.core.designsystem.theme.YukiTheme
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthorBadgeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val summary = ListingSummary(
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

    private fun render(onAuthorClick: ((String) -> Unit)?) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                ListingBadgeRow(badges = summary.toBadges(onAuthorClick = onAuthorClick))
            }
        }
    }

    @Test
    fun theAuthorBadgeReportsTheAuthorWhenTapped() {
        val tapped = mutableListOf<String>()
        render { author -> tapped += author }

        composeRule.onNodeWithText("octocat").performClick()

        assertEquals(listOf("octocat"), tapped)
    }

    @Test
    fun theAuthorBadgeIsClickableWhenAHandlerIsGiven() {
        render { }

        composeRule.onNodeWithText("octocat").assertHasClickAction()
    }

    @Test
    fun theAuthorBadgeStaysInertWithoutAHandler() {
        render(onAuthorClick = null)

        composeRule.onNodeWithText("octocat").assertHasNoClickAction()
    }

    @Test
    fun theOtherBadgesStayInertWhenTheAuthorIsClickable() {
        render { }

        composeRule.onNodeWithText(ListingCategory.Gaming.label).assertHasNoClickAction()
    }
}
