package app.yuki.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.yuki.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

private const val SETTINGS_TITLE = "Settings"
private const val SETTINGS_DESCRIPTION = "Settings"

@HiltAndroidTest
class YukiNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyTabIsReachableFromTheNavigationBar() {
        YukiTab.entries.forEach { tab ->
            composeRule.onAllNodesWithText(tab.label).onFirst().performClick()
            composeRule.onAllNodesWithText(tab.label).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun theAvatarOpensSettingsAsAFullScreen() {
        composeRule.onNodeWithContentDescription(SETTINGS_DESCRIPTION).performClick()

        composeRule.onNodeWithText(SETTINGS_TITLE).assertIsDisplayed()
    }

    @Test
    fun everyTabIsOfferedInTheBarAndSettingsIsNot() {
        YukiTab.entries.forEach { tab ->
            composeRule.onAllNodesWithText(tab.label).onFirst().assertIsDisplayed()
        }

        composeRule.onNodeWithText(SETTINGS_TITLE).assertDoesNotExist()
    }
}
