package app.yuki.feature.listing

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import app.yuki.core.designsystem.R as DesignR
import app.yuki.core.model.UiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ListingVersionSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun text(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(id, *args)

    private fun seeAllVersions(): String =
        text(DesignR.string.designsystem_section_see_all, text(R.string.listing_section_versions))

    private fun setVersions(count: Int, onVersionsSelected: () -> Unit = {}) {
        val versions = (1..count).map { index -> version("v$index.0.0") }
        val base = noopCallbacks()

        composeRule.setContent {
            ListingScreen(
                state = ListingScreenState(
                    listing = UiState.Success(detail(versions = versions).toUiModel()),
                    installStatus = idleStatus(),
                ),
                callbacks = base.copy(
                    callbacks = base.callbacks.copy(onVersionsSelected = onVersionsSelected),
                ),
                onBackClick = {},
            )
        }
    }

    private fun scrollToEnd() {
        composeRule.onNode(hasTestTag(LISTING_DETAIL_TAG))
            .performScrollToNode(hasText(text(R.string.listing_section_versions)))
    }

    @Test
    fun capsTheVersionListAtThreeRows() {
        setVersions(count = 5)
        scrollToEnd()

        composeRule.onNode(hasTestTag(LISTING_DETAIL_TAG)).performScrollToNode(hasText("Aurora v3.0.0"))
        composeRule.onAllNodesWithText("Aurora v4.0.0").assertCountEquals(0)
        composeRule.onAllNodesWithText("Aurora v5.0.0").assertCountEquals(0)
    }

    @Test
    fun seeAllOpensTheVersionsScreen() {
        var openCount = 0
        setVersions(count = 4, onVersionsSelected = { openCount += 1 })
        scrollToEnd()

        composeRule.onNodeWithContentDescription(seeAllVersions()).performClick()

        assertEquals(1, openCount)
    }

    @Test
    fun offersNoSeeAllWhenEveryVersionFits() {
        setVersions(count = 3)
        scrollToEnd()

        composeRule.onAllNodesWithContentDescription(seeAllVersions()).assertCountEquals(0)
    }
}
