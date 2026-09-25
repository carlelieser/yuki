package app.yuki.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.yuki.MainActivity
import app.yuki.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test


@HiltAndroidTest
class YukiNavigationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyTabIsReachableFromTheNavigationBar() {
        YukiTab.entries.forEach { tab ->
            composeRule.onAllNodesWithText(composeRule.activity.getString(tab.label)).onFirst().performClick()
            composeRule.onAllNodesWithText(composeRule.activity.getString(tab.label)).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun theAvatarOpensSettingsAsAFullScreen() {
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.app_settings_description)).performClick()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.app_settings_title)).assertIsDisplayed()
    }

    @Test
    fun everyTabIsOfferedInTheBarAndSettingsIsNot() {
        YukiTab.entries.forEach { tab ->
            composeRule.onAllNodesWithText(composeRule.activity.getString(tab.label)).onFirst().assertIsDisplayed()
        }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.app_settings_title)).assertDoesNotExist()
    }
}
