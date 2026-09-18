package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.AVATAR_EDIT_DESCRIPTION
import app.yuki.core.designsystem.component.AVATAR_TAG
import app.yuki.core.designsystem.component.Avatar
import app.yuki.core.designsystem.component.AvatarContent
import app.yuki.core.designsystem.component.AvatarSize
import app.yuki.core.designsystem.theme.YukiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AvatarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render(
        content: AvatarContent,
        size: AvatarSize = AvatarSize.Medium,
        onEditClick: (() -> Unit)? = null,
    ) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                Avatar(content = content, size = size, onEditClick = onEditClick)
            }
        }
    }

    @Test
    fun aSignedInAvatarShowsInitialsWhenThereIsNoPicture() {
        render(AvatarContent(imageUrl = null, displayName = "Ada Lovelace"))

        composeRule.onNodeWithText("AL").assertIsDisplayed()
    }

    @Test
    fun aSignedOutAvatarShowsThePersonGlyphRatherThanInitials() {
        render(AvatarContent(imageUrl = null, displayName = null))

        composeRule.onNodeWithTag(AVATAR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("AL").assertDoesNotExist()
    }

    @Test
    fun aPlainAvatarHasNoEditAffordance() {
        render(AvatarContent(imageUrl = null, displayName = "Ada Lovelace"))

        composeRule.onNodeWithContentDescription(AVATAR_EDIT_DESCRIPTION).assertDoesNotExist()
    }

    @Test
    fun anEditableAvatarShowsTheBadgeAndReportsClicks() {
        var clicks = 0
        render(
            content = AvatarContent(imageUrl = null, displayName = "Ada Lovelace"),
            size = AvatarSize.Large,
            onEditClick = { clicks += 1 },
        )

        composeRule.onNodeWithContentDescription(AVATAR_EDIT_DESCRIPTION).assertIsDisplayed()
        composeRule.onNodeWithTag(AVATAR_TAG).performClick()

        assertEquals(1, clicks)
    }
}
