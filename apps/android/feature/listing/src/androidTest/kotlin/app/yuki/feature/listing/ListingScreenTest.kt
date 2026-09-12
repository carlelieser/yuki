package app.yuki.feature.listing

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performScrollToNode
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.model.FailureReason
import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingVersion
import app.yuki.core.model.Screenshot
import app.yuki.core.model.downloadSizeOf
import app.yuki.core.model.UiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ListingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        listing: UiState<ListingUiModel>,
        status: ListingInstallStatus = idleStatus(),
        callbacks: ListingScreenCallbacks = noopCallbacks(),
    ) {
        composeRule.setContent {
            ListingScreen(
                state = ListingScreenState(listing = listing, installStatus = status),
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
    fun rendersTheListingIdentityOnSuccess() {
        setScreen(UiState.Success(detail().toUiModel()))

        composeRule.onNodeWithTag(LISTING_DETAIL_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("By nightsky").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("128 stars").assertIsDisplayed()
    }

    @Test
    fun rendersTheBannerWhenTheListingHasOne() {
        setScreen(UiState.Success(detail().toUiModel()))

        composeRule.onNode(hasTestTag(LISTING_BANNER_TAG)).assertIsDisplayed()
    }

    @Test
    fun omitsTheBannerWhenTheListingHasNone() {
        setScreen(UiState.Success(detail(summary = summary(bannerUrl = null)).toUiModel()))

        composeRule.onAllNodesWithTag(LISTING_BANNER_TAG).assertCountEquals(0)
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

        composeRule.onAllNodesWithText("Install").onFirst().assertIsDisplayed()
    }

    @Test
    fun replacesInstallButtonWithANoticeWhenNothingIsInstallable() {
        setScreen(UiState.Success(detail(versions = emptyList()).toUiModel()))

        composeRule.scrollToText(NO_INSTALLABLE_VERSION_TITLE)
        composeRule.onNode(hasTestTag(NO_INSTALLABLE_VERSION_TAG)).assertIsDisplayed()
        composeRule.onAllNodesWithText("Install").assertCountEquals(0)
    }

    @Test
    fun showsTheAppTitleOnlyOnceOnTheDetailScreen() {
        setScreen(UiState.Success(detail().toUiModel()))

        composeRule.onAllNodesWithText("Aurora").assertCountEquals(1)
    }

    @Test
    fun reportsTheTappedScreenshotIndex() {
        val selected = mutableListOf<Int>()
        val screenshots = listOf(
            Screenshot("https://cdn.test/one.png", "Home"),
            Screenshot("https://cdn.test/two.png", "Settings"),
        )
        setScreen(
            listing = UiState.Success(detail(screenshots = screenshots).toUiModel()),
            callbacks = withScreenshotHandler { index -> selected.add(index) },
        )

        composeRule.scrollToText("Screenshots")
        composeRule.onNodeWithContentDescription("Settings").performClick()

        assertEquals(listOf(1), selected)
    }

    @Test
    fun installsTheExactVersionWhoseRowWasTapped() {
        val requested = mutableListOf<String>()
        val versions = listOf(version("v2.0.0"), version("v1.0.0"))
        setScreen(
            listing = UiState.Success(detail(versions = versions).toUiModel()),
            callbacks = withVersionInstallHandler { _, version -> requested.add(version.tag) },
        )

        composeRule.scrollToText("Aurora v1.0.0")
        composeRule.onAllNodesWithText("Install").onLast().performClick()

        assertEquals(listOf("v1.0.0"), requested)
    }

    @Test
    fun offersNoInstallButtonForAVersionWithoutAnAsset() {
        val versions = listOf(version("v2.0.0"), version("v1.0.0", downloadUrl = null))
        setScreen(UiState.Success(detail(versions = versions).toUiModel()))

        composeRule.scrollToText(NO_ASSET_LABEL)
        composeRule.onNodeWithContentDescription(NO_ASSET_LABEL).assertIsDisplayed()
        composeRule.onAllNodesWithText("Install").assertCountEquals(1)
    }

    @Test
    fun installsAPrereleaseFromItsOwnRow() {
        val requested = mutableListOf<String>()
        val versions = listOf(version("v2.0.0"), version("v1.5.0-rc", isPrerelease = true))
        setScreen(
            listing = UiState.Success(detail(versions = versions).toUiModel()),
            callbacks = withVersionInstallHandler { _, version -> requested.add(version.tag) },
        )

        composeRule.scrollToText("Aurora v1.5.0-rc")
        composeRule.onAllNodesWithText("Install").onLast().performClick()

        assertEquals(listOf("v1.5.0-rc"), requested)
    }

    @Test
    fun disablesOtherVersionRowsWhileOneVersionInstalls() {
        val versions = listOf(version("v2.0.0"), version("v1.0.0"))
        setScreen(
            listing = UiState.Success(detail(versions = versions).toUiModel()),
            status = ListingInstallStatus(
                state = InstallState.Downloading(HALF_DOWNLOADED),
                versionTag = "v2.0.0",
            ),
        )

        composeRule.scrollToText("Aurora v1.0.0")
        composeRule.onAllNodesWithText("Install").onLast().assertIsNotEnabled()
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
    fun marksEveryLinkRowAsOpeningExternally() {
        setScreen(UiState.Success(detail().toUiModel()))

        composeRule.scrollToText("License")
        composeRule.onAllNodesWithContentDescription(EXTERNAL_LINK_DESCRIPTION)
            .assertCountEquals(linkRows(detail()).size)
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

private fun idleStatus(): ListingInstallStatus =
    ListingInstallStatus(state = InstallState.NotInstalled, versionTag = null)

private fun ComposeContentTestRule.scrollToText(text: String) {
    onNode(hasTestTag(LISTING_DETAIL_TAG)).performScrollToNode(hasText(text))
}

private fun noopCallbacks(): ListingScreenCallbacks = ListingScreenCallbacks(
    callbacks = ListingCallbacks(
        onInstallAction = InstallActionHandler { },
        onOpenLink = LinkOpener { },
        onScreenshotSelected = { },
        onVersionInstallAction = VersionInstallHandler { _, _ -> },
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

private fun withScreenshotHandler(onSelect: (Int) -> Unit): ListingScreenCallbacks {
    val base = noopCallbacks()
    return base.copy(callbacks = base.callbacks.copy(onScreenshotSelected = onSelect))
}

private fun withVersionInstallHandler(
    onAction: (app.yuki.core.designsystem.component.InstallAction, ListingVersion) -> Unit,
): ListingScreenCallbacks {
    val base = noopCallbacks()
    return base.copy(
        callbacks = base.callbacks.copy(
            onVersionInstallAction = VersionInstallHandler(onAction),
        ),
    )
}

private val HALF_DOWNLOADED = downloadSizeOf(bytesDownloaded = 500L, bytesTotal = 1_000L)
