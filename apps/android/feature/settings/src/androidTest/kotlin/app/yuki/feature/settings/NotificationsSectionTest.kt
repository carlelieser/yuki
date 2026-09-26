package app.yuki.feature.settings

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NotificationsSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun text(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun theGroupOffersTheUpdatesAvailableToggle() {
        setContent(isEnabled = true)

        composeRule.onNodeWithText(text(R.string.settings_notifications_title).uppercase())
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_update_notifications_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_update_notifications_supporting))
            .assertIsDisplayed()
    }

    @Test
    fun theToggleReflectsTheStoredChoice() {
        setContent(isEnabled = false)

        composeRule.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun tappingTheToggleReportsTheNewChoice() {
        val choices = mutableListOf<Boolean>()
        setContent(isEnabled = true, onChange = choices::add)

        composeRule.onNode(isToggleable()).assertIsOn().performClick()

        assertEquals(listOf(false), choices)
    }

    private fun setContent(isEnabled: Boolean, onChange: (Boolean) -> Unit = {}) {
        val preferences = YukiPreferences.Defaults.copy(isUpdateNotificationEnabled = isEnabled)
        val actions = PreferenceActions(
            onAutoUpdateCheckChange = {},
            onUpdateNotificationChange = onChange,
            onIncludePrereleasesChange = {},
            onInstallModeChange = {},
            onAppearanceChange = {},
            onDynamicColorChange = {},
            onInstallerPackageChange = {},
            onChooseInstallerApp = {},
        )

        composeRule.setContent {
            NotificationsSection(preferences = preferences, actions = actions)
        }
    }
}
