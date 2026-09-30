package app.yuki.feature.listing

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import app.yuki.core.designsystem.R as DesignR
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.UiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VersionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun text(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(id)

    private fun setScreen(
        listing: UiState<ListingDetail>,
        callbacks: VersionsCallbacks = noopVersionsCallbacks(),
    ) {
        composeRule.setContent {
            VersionsScreen(
                state = VersionsScreenState(listing = listing, installStatus = idleStatus()),
                callbacks = callbacks,
            )
        }
    }

    @Test
    fun listsEveryVersion() {
        val versions = (1..6).map { index -> version("v$index.0.0") }
        setScreen(UiState.Success(detail(versions = versions)))

        versions.forEach { version ->
            composeRule.onNodeWithTag(VERSIONS_LIST_TAG).performScrollToNode(hasText("Aurora ${version.tag}"))
            composeRule.onNodeWithText("Aurora ${version.tag}").assertIsDisplayed()
        }
    }

    @Test
    fun installsTheExactVersionWhoseRowWasTapped() {
        val requested = mutableListOf<String>()
        val versions = listOf(version("v2.0.0"), version("v1.0.0"))
        setScreen(
            listing = UiState.Success(detail(versions = versions)),
            callbacks = noopVersionsCallbacks().copy(
                onVersionInstallAction = VersionInstallHandler { _, version -> requested.add(version.tag) },
            ),
        )

        composeRule.onAllNodesWithText(text(DesignR.string.designsystem_install)).onLast().performClick()

        assertEquals(listOf("v1.0.0"), requested)
    }

    @Test
    fun showsFailureStateAndRetriesOnClick() {
        var retryCount = 0
        setScreen(
            listing = UiState.Failure(FailureReason.Offline),
            callbacks = noopVersionsCallbacks().copy(onRetry = { retryCount += 1 }),
        )

        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(text(DesignR.string.designsystem_failure_retry)).performClick()

        assertEquals(1, retryCount)
    }
}

private fun noopVersionsCallbacks(): VersionsCallbacks = VersionsCallbacks(
    onVersionInstallAction = VersionInstallHandler { _, _ -> },
    onRetry = {},
    onBackClick = {},
)
