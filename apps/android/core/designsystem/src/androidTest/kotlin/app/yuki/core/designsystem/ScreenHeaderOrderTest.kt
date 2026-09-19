package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.SCREEN_ACTION_TAG
import app.yuki.core.designsystem.component.ScreenAction
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiScreen
import app.yuki.core.designsystem.theme.YukiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TRAILING_TAG = "headerTrailing"

@RunWith(AndroidJUnit4::class)
class ScreenHeaderOrderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theTrailingSlotSitsOutsideTheActionButton() {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                YukiScreen(
                    title = "Explore",
                    action = ScreenAction(
                        icon = YukiIcons.Settings,
                        description = "Settings",
                        onClick = {},
                    ),
                    trailing = {
                        Box(modifier = Modifier.size(36.dp).testTag(TRAILING_TAG))
                    },
                ) {}
            }
        }

        val action = composeRule.onNodeWithTag(SCREEN_ACTION_TAG).getUnclippedBoundsInRoot()
        val trailing = composeRule.onNodeWithTag(TRAILING_TAG).getUnclippedBoundsInRoot()

        assertTrue(
            "the trailing slot should sit to the right of the action button",
            trailing.left.value > action.left.value,
        )
    }
}
