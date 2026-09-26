package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import app.yuki.core.designsystem.component.YukiTextField
import app.yuki.core.designsystem.theme.YukiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class KeyboardClearanceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun aFocusedFieldScrollsIntoViewWithRoomBelowIt() {
        val scroll = ScrollState(0)
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                Column(
                    modifier = Modifier
                        .testTag(VIEWPORT_TAG)
                        .height(VIEWPORT_HEIGHT)
                        .verticalScroll(scroll),
                ) {
                    Spacer(modifier = Modifier.height(OFFSCREEN_GAP))
                    YukiTextField(
                        value = "",
                        onValueChange = {},
                        label = "Email",
                        modifier = Modifier.testTag(FIELD_TAG),
                    )
                    Spacer(modifier = Modifier.height(OFFSCREEN_GAP))
                }
            }
        }

        composeRule.onNodeWithTag(FIELD_TAG).requestFocus()
        composeRule.waitForIdle()

        val viewport = composeRule.onNodeWithTag(VIEWPORT_TAG).getBoundsInRoot()
        val field = composeRule.onNodeWithTag(FIELD_TAG).getBoundsInRoot()
        assertTrue(scroll.value > 0)
        assertTrue(field.bottom + CLEARANCE <= viewport.bottom + TOLERANCE)
    }
}

private const val VIEWPORT_TAG = "viewport"
private const val FIELD_TAG = "field"
private val VIEWPORT_HEIGHT = 300.dp
private val OFFSCREEN_GAP = 1_000.dp
private val CLEARANCE = 48.dp
private val TOLERANCE = 1.dp
