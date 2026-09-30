package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.DETAIL_HEADING_ICON_TAG
import app.yuki.core.designsystem.component.DetailHeading
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.theme.YukiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailHeadingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun aHeadingShowsTheIconTitleAndDescription() {
        composeRule.setContent {
            YukiTheme {
                YukiDetailScreen(
                    heading = DetailHeading(title = "Yuki", description = "Ratings and reviews", iconUrl = null, hasIcon = true),
                    onBackClick = {},
                ) {}
            }
        }

        composeRule.onNodeWithTag(DETAIL_HEADING_ICON_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Yuki").assertIsDisplayed()
        composeRule.onNodeWithText("Ratings and reviews").assertIsDisplayed()
    }

    @Test
    fun aPlainTitleShowsNoIcon() {
        composeRule.setContent {
            YukiTheme { YukiDetailScreen(title = "Versions", onBackClick = {}) {} }
        }

        composeRule.onNodeWithText("Versions").assertIsDisplayed()
        composeRule.onAllNodesWithTag(DETAIL_HEADING_ICON_TAG).assertCountEquals(0)
    }
}
