package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.STATUS_ICON_TAG
import app.yuki.core.designsystem.component.StatusCard
import app.yuki.core.designsystem.component.StatusContent
import app.yuki.core.designsystem.component.StatusTone
import app.yuki.core.designsystem.theme.YukiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class StatusCardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var neutral = Color.Unspecified

    private fun setCards() {
        composeRule.setContent {
            YukiTheme {
                neutral = MaterialTheme.colorScheme.surfaceContainerHigh
                Column {
                    StatusTone.entries.forEach { tone ->
                        StatusCard(
                            content = StatusContent(title = TITLE, description = DESCRIPTION, tone = tone),
                            modifier = Modifier.testTag(tone.name),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun everyToneLeadsWithAnIcon() {
        setCards()

        StatusTone.entries.forEach { tone ->
            composeRule.onNode(hasTestTag(STATUS_ICON_TAG) and hasAnyAncestor(hasTestTag(tone.name)))
                .assertIsDisplayed()
        }
    }

    @Test
    fun everyToneSitsOnTheSameNeutralSurface() {
        setCards()

        StatusTone.entries.forEach { tone ->
            val image = composeRule.onNodeWithTag(tone.name).captureToImage().toPixelMap()
            val inset = image.width / INSET_DIVISOR
            val background = image[inset, image.height - inset]

            assertTrue("$tone card is tinted", background.isCloseTo(neutral))
        }
    }
}

private const val TITLE = "Heads up"
private const val DESCRIPTION = "Something worth knowing."
private const val INSET_DIVISOR = 40
private const val COLOR_TOLERANCE = 0.03f

private fun Color.isCloseTo(other: Color): Boolean =
    listOf(red - other.red, green - other.green, blue - other.blue).all { delta ->
        abs(delta) < COLOR_TOLERANCE
    }
