package app.yuki.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.COLLECTION_EMPTY_TAG
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.model.FailureReason
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.UiState
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
    fun aFailureRendersTheFailureStateAndNotTheEmptyState() {
        setContent(UiState.Failure(FailureReason.Offline))

        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_EMPTY_TAG).assertDoesNotExist()
    }

    private fun setContent(
        state: UiState<LibraryContent>,
        onListingClick: (String) -> Unit = {},
        onExploreClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            LibraryContentScreen(
                state = state,
                actions = LibraryActions(
                    onListingClick = onListingClick,
                    onExploreClick = onExploreClick,
                ),
            )
        }
    }
}

private fun item(): LibraryItem = LibraryItem(app = TERMUX, canOpen = true)

private val TERMUX = InstalledApp(
    githubRepoId = 1_234L,
    packageName = "com.termux",
    slug = "termux",
    title = "Termux",
    iconUrl = null,
    versionTag = "v0.118.0",
)
