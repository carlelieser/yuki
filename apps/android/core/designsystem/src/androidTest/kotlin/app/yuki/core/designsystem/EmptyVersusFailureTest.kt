package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.COLLECTION_EMPTY_TAG
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FAILURE_RETRY_LABEL
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.theme.YukiTheme
import app.yuki.core.model.FailureReason
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmptyVersusFailureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun emptyStateIsNotRenderedAsAFailure() {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                CollectionEmpty(
                    content = EmptyContent(
                        title = "Everything is up to date",
                        description = "Yuki will tell you when an update lands.",
                    ),
                )
            }
        }

        composeRule.onNodeWithTag(COLLECTION_EMPTY_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertDoesNotExist()
        composeRule.onNodeWithText(FAILURE_RETRY_LABEL).assertDoesNotExist()
    }

    @Test
    fun failureStateIsNotRenderedAsEmpty() {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                FailureState(reason = FailureReason.Offline, onRetry = { })
            }
        }

        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_EMPTY_TAG).assertDoesNotExist()
        composeRule.onNodeWithText("No connection").assertIsDisplayed()
    }

    @Test
    fun offlineAndServerFailuresReadDifferently() {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                FailureState(reason = FailureReason.Server(status = 503))
            }
        }

        composeRule.onNodeWithText("Server error").assertIsDisplayed()
        composeRule.onNodeWithText("No connection").assertDoesNotExist()
    }

    @Test
    fun failureStateRetryIsInvoked() {
        var retryCount = 0
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                FailureState(reason = FailureReason.NotFound, onRetry = { retryCount += 1 })
            }
        }

        composeRule.onNodeWithText(FAILURE_RETRY_LABEL).performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun emptyStateActionIsInvoked() {
        var browseCount = 0
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                CollectionEmpty(
                    content = EmptyContent(
                        title = "Nothing installed yet",
                        description = "Apps you install through Yuki show up here.",
                        actionLabel = "Browse apps",
                        onAction = { browseCount += 1 },
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Browse apps").performClick()

        assertEquals(1, browseCount)
    }
}
