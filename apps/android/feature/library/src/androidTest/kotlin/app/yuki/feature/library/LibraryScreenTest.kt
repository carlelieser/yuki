package app.yuki.feature.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.APP_ICON_PROGRESS_TAG
import app.yuki.core.designsystem.component.COLLECTION_EMPTY_TAG
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.designsystem.component.PULL_TO_REFRESH_TAG
import app.yuki.core.model.FailureReason
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.LibraryEntry
import app.yuki.core.model.InstallState
import app.yuki.core.model.UiState
import app.yuki.core.model.downloadSizeOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anEmptyLibraryRendersTheEmptyStateAndNotAFailure() {
        setContent(UiState.Success(LibraryContent(emptyList())))

        composeRule.onNodeWithTag(COLLECTION_EMPTY_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertDoesNotExist()
    }

    @Test
    fun theEmptyStateActionPointsAtExplore() {
        var exploreClicks = 0
        setContent(UiState.Success(LibraryContent(emptyList())), onExploreClick = { exploreClicks++ })

        composeRule.onNodeWithText(LIBRARY_EMPTY_ACTION).performClick()

        assertEquals(1, exploreClicks)
    }

    @Test
    fun installedAppsRenderAsRowsThatOpenTheirListing() {
        val clicked = mutableListOf<String>()
        setContent(UiState.Success(LibraryContent(listOf(item()))), onListingClick = clicked::add)

        composeRule.onNodeWithText(TERMUX.title).assertIsDisplayed()
        composeRule.onNodeWithText(TERMUX.versionTag).assertIsDisplayed()
        composeRule.onNodeWithText(TERMUX.title).performClick()

        assertEquals(listOf(TERMUX.slug), clicked)
    }

    @Test
    fun anInstallingRowReportsProgressAroundItsIcon() {
        setContent(UiState.Success(LibraryContent(listOf(item(InstallState.Installing)))))

        composeRule.onNodeWithTag(APP_ICON_PROGRESS_TAG).assertIsDisplayed()
    }

    @Test
    fun aDownloadingRowShowsOnlyTheIconProgress() {
        setContent(UiState.Success(LibraryContent(listOf(item(InstallState.Downloading(PARTLY_DOWNLOADED))))))

        composeRule.onAllNodesWithTag(APP_ICON_PROGRESS_TAG).assertCountEquals(1)
    }

    @Test
    fun anInstallingRowSaysInstallingRatherThanTheVersion() {
        setContent(UiState.Success(LibraryContent(listOf(item(InstallState.Installing)))))

        composeRule.onNodeWithText(LIBRARY_INSTALLING_SUPPORTING).assertIsDisplayed()
        composeRule.onNodeWithText(TERMUX.versionTag).assertDoesNotExist()
    }

    @Test
    fun aFailedRowOffersDismiss() {
        val dismissed = mutableListOf<Long>()
        setContent(
            UiState.Success(
                LibraryContent(listOf(item(InstallState.Failed(InstallFailure.TimedOut)))),
            ),
            onDismiss = dismissed::add,
        )

        composeRule.onNodeWithTag(LIBRARY_DISMISS_TAG).performClick()

        assertEquals(listOf(TERMUX.githubRepoId), dismissed)
    }

    @Test
    fun aFailureRendersTheFailureStateAndNotTheEmptyState() {
        setContent(UiState.Failure(FailureReason.Offline))

        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_EMPTY_TAG).assertDoesNotExist()
    }

    private fun setContent(
        state: UiState<LibraryContent>,
        onListingClick: (String) -> Unit = {},
        onExploreClick: () -> Unit = {},
        onPullToRefresh: () -> Unit = {},
        onDismiss: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            LibraryContentScreen(
                state = state,
                refresh = LibraryRefresh(
                    isRefreshing = false,
                    onPullToRefresh = onPullToRefresh,
                ),
                actions = LibraryActions(
                    onListingClick = onListingClick,
                    onExploreClick = onExploreClick,
                    onDismiss = onDismiss,
                    onFilterSelected = {},
                ),
                contentPadding = PaddingValues(),
            )
        }
    }

    @Test
    fun pullingDownTheLibraryRequestsARefresh() {
        var refreshes = 0
        setContent(
            UiState.Success(LibraryContent(listOf(item()))),
            onPullToRefresh = { refreshes += 1 },
        )

        composeRule.onNodeWithTag(PULL_TO_REFRESH_TAG).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertEquals(1, refreshes)
    }

    @Test
    fun theFilterControlSitsInTheHeaderAndNamesTheActiveFilter() {
        setContent(
            UiState.Success(
                LibraryContent(items = listOf(item()), filter = LibraryFilter.NotInstalled),
            ),
        )

        composeRule
            .onNodeWithContentDescription("$LIBRARY_FILTER_DESCRIPTION Not installed")
            .assertIsDisplayed()
    }

    @Test
    fun theEmptyLibraryOffersNoFilterToApply() {
        setContent(UiState.Success(LibraryContent(emptyList())))

        composeRule.onNodeWithTag(LIBRARY_FILTER_TAG).assertDoesNotExist()
    }

    @Test
    fun aFilterThatMatchesNothingIsNotShownAsAnEmptyLibrary() {
        setContent(
            UiState.Success(
                LibraryContent(items = listOf(item()), filter = LibraryFilter.NotInstalled),
            ),
        )

        composeRule.onNodeWithText(LIBRARY_NO_MATCHES_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(LIBRARY_EMPTY_TITLE).assertDoesNotExist()
    }

    @Test
    fun pullingDownTheEmptyLibraryRequestsARefresh() {
        var refreshes = 0
        setContent(
            UiState.Success(LibraryContent(emptyList())),
            onPullToRefresh = { refreshes += 1 },
        )

        composeRule.onNodeWithTag(PULL_TO_REFRESH_TAG).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertEquals(1, refreshes)
    }
}

private fun item(install: InstallState = InstallState.NotInstalled): LibraryItem = LibraryItem(
    entry = TERMUX,
    presence = LibraryPresence.Installed,
    install = install,
)

private val PARTLY_DOWNLOADED =
    downloadSizeOf(bytesDownloaded = 4_100_000L, bytesTotal = 12_100_000L)

private val TERMUX = LibraryEntry(
    githubRepoId = 1_234L,
    packageName = "com.termux",
    slug = "termux",
    title = "Termux",
    iconUrl = null,
    versionTag = "v0.118.0",
)
