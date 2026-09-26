package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.material3.Icon
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.NAV_BADGE_TAG
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiNavBar
import app.yuki.core.designsystem.component.YukiNavBarItem
import app.yuki.core.designsystem.component.YukiNavDestination
import app.yuki.core.designsystem.theme.YukiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavBadgeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun showItemWithBadge(count: Int) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                YukiNavBar {
                    YukiNavBarItem(
                        destination = YukiNavDestination(
                            label = "Updates",
                            icon = { Icon(imageVector = YukiIcons.Update, contentDescription = null) },
                            badgeCount = count,
                        ),
                        isSelected = false,
                        onSelect = {},
                    )
                }
            }
        }
    }

    @Test
    fun noPendingItemsShowsNoBadge() {
        showItemWithBadge(0)

        composeRule.onNodeWithTag(NAV_BADGE_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun pendingItemsShowTheirCount() {
        showItemWithBadge(3)

        composeRule.onNodeWithTag(NAV_BADGE_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("3", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun countsAboveNinetyNineAreCapped() {
        showItemWithBadge(150)

        composeRule.onNodeWithText("99+", useUnmergedTree = true).assertIsDisplayed()
    }
}
