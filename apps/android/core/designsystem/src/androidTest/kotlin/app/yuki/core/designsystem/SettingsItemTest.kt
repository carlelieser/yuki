package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.SettingsItem
import app.yuki.core.designsystem.component.SettingsItemContent
import app.yuki.core.designsystem.theme.YukiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsItemTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render(content: SettingsItemContent, onClick: (() -> Unit)? = null) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                SettingsItem(content = content, onClick = onClick)
            }
        }
    }

    private fun trailingLabel(label: String): @Composable () -> Unit = { Text(text = label) }

    @Test
    fun grantedPermissionShowsReasonAndStatus() {
        render(
            SettingsItemContent(
                title = "Install apps",
                supporting = "Install apps when Shizuku is unavailable",
                trailing = trailingLabel("Granted"),
            ),
        )

        composeRule.onNodeWithText("Install apps").assertIsDisplayed()
        composeRule.onNodeWithText("Install apps when Shizuku is unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Granted").assertIsDisplayed()
    }

    @Test
    fun deniedPermissionShowsDeniedStatus() {
        render(
            SettingsItemContent(
                title = "Notifications",
                supporting = "Notify when a download or install finishes",
                trailing = trailingLabel("Denied"),
            ),
        )

        composeRule.onNodeWithText("Denied").assertIsDisplayed()
    }

    @Test
    fun toggleRowRendersItsSwitch() {
        render(
            SettingsItemContent(
                title = "Include prereleases",
                trailing = { Switch(checked = false, onCheckedChange = { }) },
            ),
        )

        composeRule.onNodeWithText("Include prereleases").assertIsDisplayed()
        composeRule.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun plainRowRendersWithoutSupportingText() {
        render(SettingsItemContent(title = "Open Shizuku"))

        composeRule.onNodeWithText("Open Shizuku").assertIsDisplayed()
    }

    @Test
    fun clickableRowEmitsItsClick() {
        var clickCount = 0
        render(SettingsItemContent(title = "Request permission"), onClick = { clickCount += 1 })

        composeRule.onNodeWithText("Request permission").performClick()

        assertEquals(1, clickCount)
    }
}
