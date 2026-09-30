package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.RATING_INPUT_TAG
import app.yuki.core.designsystem.component.RatingInput
import app.yuki.core.designsystem.component.RatingStarsSize
import app.yuki.core.designsystem.component.RatingStars
import app.yuki.core.designsystem.component.TextAreaContent
import app.yuki.core.designsystem.component.YukiTextArea
import app.yuki.core.designsystem.theme.YukiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RatingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun stars(count: Int): String =
        composeRule.activity.resources.getQuantityString(R.plurals.designsystem_rating_stars, count, count)

    @Test
    fun ratingStarsDescribeTheAverage() {
        composeRule.setContent { YukiTheme { RatingStars(value = 4.25) } }

        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.designsystem_badge_rating, "4.3"),
        ).assertIsDisplayed()
    }

    private var primary = Color.Unspecified

    private fun isStarCenterFilled(tag: String, starIndex: Int): Boolean {
        val image = composeRule.onNodeWithTag(tag).captureToImage().toPixelMap()
        val starWidth = image.width / STAR_COUNT
        val center = image[starWidth * starIndex + starWidth / 2, image.height / 2]

        return center.isCloseTo(primary)
    }

    @Test
    fun ratedStarsAreFilledAndTheRestAreNot() {
        composeRule.setContent {
            YukiTheme {
                primary = MaterialTheme.colorScheme.primary
                RatingStars(
                    value = 3.0,
                    size = RatingStarsSize.Large,
                    modifier = Modifier.testTag(STARS_TAG),
                )
            }
        }

        assertTrue(isStarCenterFilled(STARS_TAG, starIndex = 2))
        assertFalse(isStarCenterFilled(STARS_TAG, starIndex = 3))
    }

    @Test
    fun choosingARatingFillsTheChosenStars() {
        composeRule.setContent {
            YukiTheme {
                primary = MaterialTheme.colorScheme.primary
                var rating by remember { mutableIntStateOf(0) }
                RatingInput(value = rating, onValueChange = { next -> rating = next })
            }
        }

        composeRule.onNodeWithContentDescription(stars(4)).performClick()

        assertTrue(isStarCenterFilled(RATING_INPUT_TAG, starIndex = 3))
        assertFalse(isStarCenterFilled(RATING_INPUT_TAG, starIndex = 4))
    }

    @Test
    fun tappingAStarSelectsThatRating() {
        var chosen = 0
        composeRule.setContent {
            YukiTheme {
                var rating by remember { mutableIntStateOf(0) }
                RatingInput(value = rating, onValueChange = { next -> rating = next; chosen = next })
            }
        }

        composeRule.onNodeWithContentDescription(stars(4)).performClick()

        assertEquals(4, chosen)
        composeRule.onNodeWithContentDescription(stars(4)).assertIsSelected()
        composeRule.onNodeWithContentDescription(stars(5)).assertIsNotSelected()
    }

    @Test
    fun textAreaStopsAtItsLimitAndCountsCharacters() {
        composeRule.setContent {
            YukiTheme {
                var text by remember { mutableStateOf("") }
                YukiTextArea(
                    content = TextAreaContent(value = text, placeholder = "Write", maxLength = 5),
                    onValueChange = { next -> text = next },
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("abcdefgh")

        composeRule.onNodeWithText("abcde").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.designsystem_text_length, 5, 5))
            .assertIsDisplayed()
    }
}

private const val STARS_TAG = "ratingStars"
private const val STAR_COUNT = 5
private const val COLOR_TOLERANCE = 0.08f

private fun Color.isCloseTo(other: Color): Boolean =
    listOf(red - other.red, green - other.green, blue - other.blue).all { delta ->
        kotlin.math.abs(delta) < COLOR_TOLERANCE
    }
