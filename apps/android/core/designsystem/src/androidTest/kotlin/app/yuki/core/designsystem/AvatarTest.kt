package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.AVATAR_GLYPH_TAG
import app.yuki.core.designsystem.component.AVATAR_TAG
import app.yuki.core.designsystem.component.AccountAvatar
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

    private fun text(@StringRes id: Int, vararg args: Any): String =
        composeRule.activity.getString(id, *args)

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

        composeRule.onNodeWithContentDescription(text(R.string.designsystem_avatar_edit)).assertDoesNotExist()
    }

    @Test
    fun anEditableAvatarShowsTheBadgeAndReportsClicks() {
        var clicks = 0
        render(
            content = AvatarContent(imageUrl = null, displayName = "Ada Lovelace"),
            size = AvatarSize.Large,
            onEditClick = { clicks += 1 },
        )

        composeRule.onNodeWithContentDescription(text(R.string.designsystem_avatar_edit)).assertIsDisplayed()
        composeRule.onNodeWithTag(AVATAR_TAG).performClick()

        assertEquals(1, clicks)
    }

    private fun renderInHeaderButton(content: AvatarContent, size: AvatarSize) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                Box(
                    modifier = Modifier.size(48.dp).clickable(onClick = {}),
                    contentAlignment = Alignment.Center,
                ) {
                    AccountAvatar(
                        content = content,
                        size = size,
                        modifier = Modifier.testTag(COMPACT_AVATAR_TAG),
                    )
                }
            }
        }
    }

    @Test
    fun aCompactAvatarIsLargerThanTheGlyphItHolds() {
        renderInHeaderButton(
            content = AvatarContent(imageUrl = null, displayName = "Ada Lovelace"),
            size = AvatarSize.Compact,
        )

        composeRule.onNodeWithTag(COMPACT_AVATAR_TAG, useUnmergedTree = true)
            .assertWidthIsEqualTo(36.dp)
            .assertHeightIsEqualTo(36.dp)
    }

    @Test
    fun aCompactPersonGlyphMatchesTheSiblingHeaderIcons() {
        renderInHeaderButton(
            content = AvatarContent(imageUrl = null, displayName = null),
            size = AvatarSize.Compact,
        )

        composeRule.onNodeWithTag(AVATAR_GLYPH_TAG, useUnmergedTree = true)
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
    }

    @Test
    fun aSignedOutAvatarKeepsTheCompactGlyphSize() {
        renderInHeaderButton(
            content = AvatarContent(imageUrl = null, displayName = null),
            size = AvatarSize.Small,
        )

        composeRule.onNodeWithTag(COMPACT_AVATAR_TAG, useUnmergedTree = true)
            .assertWidthIsEqualTo(24.dp)
    }
}

private const val COMPACT_AVATAR_TAG = "compactAvatar"
