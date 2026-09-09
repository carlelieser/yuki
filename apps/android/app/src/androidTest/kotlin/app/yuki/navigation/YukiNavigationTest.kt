package app.yuki.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.yuki.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

private const val SETTINGS_TITLE = "Settings"

@HiltAndroidTest
class YukiNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyTabIsReachableFromTheNavigationBar() {
        YukiTab.entries.forEach { tab ->
            composeRule.onNodeWithText(tab.label).performClick()
            composeRule.onNodeWithText(tab.label).assertIsDisplayed()
        }
    }

    @Test
    fun theGearActionOpensSettingsAsAFullScreen() {
        composeRule.onNodeWithTag(SETTINGS_ACTION_TAG).performClick()

        composeRule.onNodeWithText(SETTINGS_TITLE).assertIsDisplayed()
    }

    @Test
    fun settingsIsNotOfferedAsAFourthBarDestination() {
        YukiTab.entries.forEach { tab ->
            composeRule.onNodeWithText(tab.label).assertIsDisplayed()
        }

        composeRule.onNodeWithText(SETTINGS_TITLE).assertDoesNotExist()
    }
}
