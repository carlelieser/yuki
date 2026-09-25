package app.yuki.install

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import app.yuki.R
import app.yuki.core.designsystem.R as DesignR
import app.yuki.core.designsystem.component.LocalYukiSnackbarHostState
import app.yuki.core.designsystem.component.YukiSnackbarHost
import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InstallFailureSnackbarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val retried = mutableListOf<InstallProgress>()
    private val dismissed = mutableListOf<InstallProgress>()
    private val allowed = mutableListOf<InstallProgress>()

    @Test
    fun aFailedInstallShowsItsMessage() {
        render(FAILED)

        composeRule.onNodeWithText(
            text(DesignR.string.designsystem_install_error_download_gone, TITLE),
        ).assertIsDisplayed()
    }

    @Test
    fun tappingRetryRetriesTheFailedInstall() {
        render(FAILED)

        composeRule.onNodeWithText(text(DesignR.string.designsystem_install_retry)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(FAILED), retried)
        assertEquals(emptyList<InstallProgress>(), dismissed)
    }

    @Test
    fun dismissingClearsTheFailure() {
        render(FAILED)

        composeRule.onNodeWithContentDescription(DISMISS, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(FAILED), dismissed)
        assertEquals(emptyList<InstallProgress>(), retried)
    }

    @Test
    fun aMissingInstallPermissionOffersToAllowInstalls() {
        render(UNPERMITTED)

        composeRule.onNodeWithText(text(R.string.app_install_permission_allow)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(UNPERMITTED), allowed)
        assertEquals(emptyList<InstallProgress>(), retried)
    }

    private fun render(failed: InstallProgress?) {
        val hostState = SnackbarHostState()

        composeRule.setContent {
            CompositionLocalProvider(LocalYukiSnackbarHostState provides hostState) {
                Box {
                    InstallFailureSnackbar(
                        failed = failed,
                        actions = InstallFailureActions(
                            onRetry = retried::add,
                            onDismiss = dismissed::add,
                            onAllowInstalls = allowed::add,
                        ),
                    )
                    YukiSnackbarHost(hostState = hostState)
                }
            }
        }
    }
}

private const val TITLE = "Termux"
private const val DISMISS = "Dismiss"

private val FAILED = InstallProgress(
    target = InstallTarget(githubRepoId = 7L, slug = "termux", title = TITLE, iconUrl = null),
    versionTag = "v1",
    state = InstallState.Failed(InstallFailure.DownloadFailed(httpStatus = 404)),
)

private val UNPERMITTED = FAILED.copy(
    state = InstallState.Failed(InstallFailure.InstallPermissionMissing),
)

private fun text(id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)
