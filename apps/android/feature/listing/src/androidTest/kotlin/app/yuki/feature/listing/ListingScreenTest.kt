package app.yuki.feature.listing

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollToNode
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.model.FailureReason
import app.yuki.core.model.InstallState
import app.yuki.core.model.UiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ListingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        listing: UiState<ListingUiModel>,
        installState: InstallState = InstallState.NotInstalled,
        callbacks: ListingScreenCallbacks = noopCallbacks(),
    ) {
        composeRule.setContent {
            ListingScreen(
                state = ListingScreenState(listing = listing, installState = installState),
                callbacks = callbacks,
                onBackClick = {},
            )
        }
    }

    @Test
    fun showsLoadingIndicatorWhileLoading() {
        setScreen(UiState.Loading)

        composeRule.onNodeWithTag(LISTING_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun showsFailureStateAndRetriesOnClick() {
        var retryCount = 0
        setScreen(
            listing = UiState.Failure(FailureReason.NotFound),
            callbacks = noopCallbacks().copy(onRetry = { retryCount += 1 }),
        )

        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun rendersTitleAuthorAndStarsOnSuccess() {
        setScreen(UiState.Success(detail().toUiModel()))

        composeRule.onNodeWithText("Aurora").assertIsDisplayed()
        composeRule.onNodeWithText("nightsky").assertIsDisplayed()
        composeRule.onNodeWithText("128 stars").assertIsDisplayed()
    }

    @Test
    fun showsArchivedWarningForAnArchivedListing() {
        setScreen(UiState.Success(detail(isArchived = true).toUiModel()))

        composeRule.scrollToText(ARCHIVED_TITLE)
        composeRule.onNode(hasTestTag(ARCHIVED_WARNING_TAG)).assertIsDisplayed()
    }

    @Test
    fun hidesArchivedWarningForAnActiveListing() {
        setScreen(UiState.Success(detail(isArchived = false).toUiModel()))

        composeRule.onAllNodesWithText(ARCHIVED_TITLE).assertCountEquals(0)
    }

    @Test
    fun showsInstallButtonWhenAVersionIsInstallable() {
        setScreen(UiState.Success(detail().toUiModel()))

        composeRule.scrollToText("Install")
        composeRule.onNodeWithText("Install").assertIsDisplayed()
    }

    @Test
    fun replacesInstallButtonWithANoticeWhenNothingIsInstallable() {
        setScreen(UiState.Success(detail(versions = emptyList()).toUiModel()))

        composeRule.scrollToText(NO_INSTALLABLE_VERSION_TITLE)
        composeRule.onNode(hasTestTag(NO_INSTALLABLE_VERSION_TAG)).assertIsDisplayed()
        composeRule.onAllNodesWithText("Install").assertCountEquals(0)
    }

    @Test
    fun forwardsTheInstallActionFromTheButton() {
        val actions = mutableListOf<String>()
        setScreen(
            listing = UiState.Success(detail().toUiModel()),
            callbacks = withInstallHandler { action -> actions.add(action.name) },
        )

        composeRule.scrollToText("Install")
        composeRule.onNodeWithText("Install").performClick()

        assertEquals(listOf("Install"), actions)
    }

    @Test
    fun opensARepositoryLinkWhenItsRowIsTapped() {
        val opened = mutableListOf<String>()
        setScreen(
            listing = UiState.Success(detail().toUiModel()),
            callbacks = withLinkOpener { url -> opened.add(url) },
        )

        composeRule.scrollToText("Repository")
        composeRule.onNodeWithText("Repository").performClick()

        assertEquals(listOf("https://github.com/nightsky/aurora"), opened)
    }

    @Test
    fun rendersEachVersionInTheVersionList() {
        val versions = listOf(version("v2.0.0"), version("v1.0.0-rc", isPrerelease = true))
        setScreen(UiState.Success(detail(versions = versions).toUiModel()))

        composeRule.scrollToText("Aurora v2.0.0")
        composeRule.onNodeWithText("Aurora v2.0.0").assertIsDisplayed()

        composeRule.scrollToText("Aurora v1.0.0-rc")
        composeRule.onNodeWithText("Aurora v1.0.0-rc").assertIsDisplayed()
        composeRule.onNodeWithText("Prerelease").assertIsDisplayed()
    }
}

private fun ComposeContentTestRule.scrollToText(text: String) {
    onNode(hasTestTag(LISTING_DETAIL_TAG)).performScrollToNode(hasText(text))
}

private fun noopCallbacks(): ListingScreenCallbacks = ListingScreenCallbacks(
    callbacks = ListingCallbacks(
        onInstallAction = InstallActionHandler { },
        onOpenLink = LinkOpener { },
    ),
    onRetry = { },
)

private fun withInstallHandler(
    onAction: (app.yuki.core.designsystem.component.InstallAction) -> Unit,
): ListingScreenCallbacks {
    val base = noopCallbacks()
    return base.copy(
        callbacks = base.callbacks.copy(onInstallAction = InstallActionHandler(onAction)),
    )
}

private fun withLinkOpener(onOpen: (String) -> Unit): ListingScreenCallbacks {
    val base = noopCallbacks()
    return base.copy(callbacks = base.callbacks.copy(onOpenLink = LinkOpener(onOpen)))
}
