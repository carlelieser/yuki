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
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.RatingInput
import app.yuki.core.designsystem.component.RatingStars
import app.yuki.core.designsystem.component.TextAreaContent
import app.yuki.core.designsystem.component.YukiTextArea
import app.yuki.core.designsystem.theme.YukiTheme
import org.junit.Assert.assertEquals
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
