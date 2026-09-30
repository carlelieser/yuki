package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.STATUS_ICON_TAG
import app.yuki.core.designsystem.component.StatusCard
import app.yuki.core.designsystem.component.StatusContent
import app.yuki.core.designsystem.component.StatusTone
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatusCardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun leadsWithItsIconWhenGivenOne() {
        composeRule.setContent {
            YukiTheme {
                StatusCard(
                    content = StatusContent(
                        title = TITLE,
                        description = DESCRIPTION,
                        tone = StatusTone.Informative,
                        icon = YukiIcons.Info,
                    ),
                )
            }
        }

        composeRule.onNodeWithTag(STATUS_ICON_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(DESCRIPTION).assertIsDisplayed()
    }

    @Test
    fun showsNoIconByDefault() {
        composeRule.setContent {
            YukiTheme { StatusCard(content = StatusContent(title = TITLE, description = DESCRIPTION)) }
        }

        composeRule.onAllNodesWithTag(STATUS_ICON_TAG).assertCountEquals(0)
    }
}

private const val TITLE = "Heads up"
private const val DESCRIPTION = "Something worth knowing."
