package app.yuki.feature.updates

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.COLLECTION_EMPTY_TAG
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.designsystem.component.PULL_TO_REFRESH_TAG
import app.yuki.core.model.AvailableUpdate
import app.yuki.core.model.FailureReason
import app.yuki.core.model.InstallState
import app.yuki.core.model.UiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdatesScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun noUpdatesRendersTheEmptyStateAndNotAFailure() {
        setContent(UiState.Success(UpdatesContent(emptyList(), emptyList())))

        composeRule.onNodeWithTag(COLLECTION_EMPTY_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(UPDATES_EMPTY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertDoesNotExist()
    }

    @Test
    fun anAvailableUpdateRendersTheVersionTransition() {
        setContent(UiState.Success(UpdatesContent(listOf(termuxRow()), emptyList())))

        composeRule.onNodeWithText("Termux").assertIsDisplayed()
        composeRule.onNodeWithText("v0.118.0 $VERSION_ARROW v0.119.0").assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_EMPTY_TAG).assertDoesNotExist()
    }

    @Test
    fun anUncheckedAppRendersInlineWithoutAWholeScreenFailure() {
        setContent(
            UiState.Success(
                UpdatesContent(
                    updates = listOf(termuxRow()),
                    unchecked = listOf(UncheckedApp(AURORA, FailureReason.Offline)),
                ),
            ),
        )

        composeRule.onNodeWithText("Termux").assertIsDisplayed()
        composeRule.onNodeWithTag(UNCHECKED_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(UNCHECKED_LABEL).assertIsDisplayed()
        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertDoesNotExist()
    }

    @Test
    fun everyListingFailingStillShowsRowsRatherThanAFailureScreen() {
        setContent(
            UiState.Success(
                UpdatesContent(
                    updates = emptyList(),
                    unchecked = listOf(UncheckedApp(AURORA, FailureReason.Offline)),
                ),
            ),
        )

        composeRule.onNodeWithTag(UNCHECKED_ROW_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(UPDATES_PARTIAL_DESCRIPTION).assertIsDisplayed()
        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertDoesNotExist()
    }

    @Test
    fun theUpdateCtaReportsTheRowItBelongsTo() {
        val actions = mutableListOf<Pair<Long, InstallAction>>()
        setContent(
            state = UiState.Success(UpdatesContent(listOf(termuxRow()), emptyList())),
            onInstallAction = { repoId, action -> actions += repoId to action },
        )

        composeRule.onNodeWithText("Update").performClick()

        assertEquals(listOf(TERMUX.githubRepoId to InstallAction.Update), actions)
    }

    @Test
    fun aWholeScreenFailureRendersTheFailureStateAndNotTheEmptyState() {
        setContent(UiState.Failure(FailureReason.Offline))

        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_EMPTY_TAG).assertDoesNotExist()
    }

    @Test
    fun pullingDownTheUpdateListRequestsARefresh() {
        var refreshes = 0
        setContent(
            UiState.Success(UpdatesContent(listOf(termuxRow()), emptyList())),
            onPullToRefresh = { refreshes += 1 },
        )

        composeRule.onNodeWithTag(UPDATES_LIST_TAG).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertEquals(1, refreshes)
    }

    @Test
    fun pullingDownTheUpToDateStateRequestsARefresh() {
        var refreshes = 0
        setContent(
            UiState.Success(UpdatesContent(emptyList(), emptyList())),
            onPullToRefresh = { refreshes += 1 },
        )

        composeRule.onNodeWithTag(PULL_TO_REFRESH_TAG).performTouchInput { swipeDown() }
        composeRule.waitForIdle()

        assertEquals(1, refreshes)
    }

    private fun setContent(
        state: UiState<UpdatesContent>,
        onInstallAction: (Long, InstallAction) -> Unit = { _, _ -> },
        onPullToRefresh: () -> Unit = {},
    ) {
        composeRule.setContent {
            UpdatesContentScreen(
                state = state,
                refresh = UpdatesRefresh(
                    isRefreshing = false,
                    onPullToRefresh = onPullToRefresh,
                ),
                actions = UpdatesActions(
                    onListingClick = {},
                    onInstallAction = onInstallAction,
                    onRetry = {},
                ),
                contentPadding = PaddingValues(),
            )
        }
    }
}

private fun termuxRow(): UpdateRow {
    val update = AvailableUpdate(installed = TERMUX, version = version("v0.119.0"))

    return UpdateRow(update = update, install = update.toInstallState())
}

private val TERMUX = installedApp(1_234L, "termux", "v0.118.0")

private val AURORA = installedApp(5_678L, "aurora-store", "4.6.4")
