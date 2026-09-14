package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.designsystem.component.InstallButton
import app.yuki.core.designsystem.component.InstallProgressPosition
import app.yuki.core.designsystem.component.InstallProgressShape
import app.yuki.core.designsystem.theme.YukiTheme
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallButtonTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render(state: InstallState, onAction: InstallActionHandler = InstallActionHandler { }) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                InstallButton(state = state, onAction = onAction)
            }
        }
    }

    private fun renderCircular(state: InstallState, position: InstallProgressPosition) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                InstallButton(
                    state = state,
                    onAction = InstallActionHandler { },
                    progressShape = InstallProgressShape.Circular,
                    progressPosition = position,
                )
            }
        }
    }

    @Test
    fun aCircularDownloadDropsTheSizeLabel() {
        renderCircular(
            state = InstallState.Downloading(PARTLY_DOWNLOADED),
            position = InstallProgressPosition.Leading,
        )

        composeRule.onNodeWithText("4.1 MB / 12.1 MB").assertDoesNotExist()
        composeRule.onNodeWithText("Cancel").assertIsEnabled()
    }

    @Test
    fun aCircularDownloadStillDescribesProgress() {
        renderCircular(
            state = InstallState.Downloading(PARTLY_DOWNLOADED),
            position = InstallProgressPosition.Trailing,
        )

        composeRule
            .onNodeWithContentDescription("Downloading, 33 percent, 4.1 MB / 12.1 MB")
            .assertExists()
    }

    @Test
    fun notInstalledOffersInstall() {
        render(InstallState.NotInstalled)

        composeRule.onNodeWithText("Install").assertIsEnabled()
    }

    @Test
    fun downloadingOffersCancel() {
        render(InstallState.Downloading(PARTLY_DOWNLOADED))

        composeRule.onNodeWithText("Cancel").assertIsEnabled()
    }

    @Test
    fun downloadingShowsTheTransferredBytes() {
        render(InstallState.Downloading(PARTLY_DOWNLOADED))

        composeRule.onNodeWithText("4.1 MB / 12.1 MB").assertExists()
    }

    @Test
    fun aDownloadWithAnUnknownTotalStillReportsTheBytesReceived() {
        render(InstallState.Downloading(UNKNOWN_TOTAL))

        composeRule.onNodeWithText("4.1 MB / ?").assertExists()
    }

    @Test
    fun anUnreadableDownloadIsNotReportedAsIncompatible() {
        render(InstallState.Failed(InstallFailure.DownloadUnreadable))

        composeRule.onNodeWithText("Download incomplete").assertExists()
        composeRule.onNodeWithText("Not compatible").assertDoesNotExist()
    }

    @Test
    fun pendingUserActionCanStillBeCancelled() {
        val actions = mutableListOf<InstallAction>()
        render(InstallState.PendingUserAction, InstallActionHandler { action -> actions += action })

        composeRule.onNodeWithText("Waiting for confirmation").assertIsEnabled().performClick()

        assertEquals(listOf(InstallAction.Cancel), actions)
    }

    @Test
    fun installingShowsItsLabel() {
        render(InstallState.Installing)

        composeRule.onNodeWithText("Installing").assertExists()
    }

    @Test
    fun installingCannotBeCancelled() {
        val actions = mutableListOf<InstallAction>()
        render(InstallState.Installing, InstallActionHandler { action -> actions += action })

        composeRule.onNodeWithText("Installing").assertIsNotEnabled().performClick()

        assertEquals(emptyList<InstallAction>(), actions)
    }

    @Test
    fun installedOffersOpen() {
        render(InstallState.Installed(versionTag = "v1.2.0"))

        composeRule.onNodeWithText("Open").assertIsEnabled()
    }

    @Test
    fun updateAvailableOffersUpdate() {
        render(InstallState.UpdateAvailable(from = "v1.0.0", to = "v1.2.0"))

        composeRule.onNodeWithText("Update").assertIsEnabled()
    }

    @Test
    fun failedOffersRetryAndNamesTheFailure() {
        render(InstallState.Failed(reason = InstallFailure.InsufficientStorage))

        composeRule.onNodeWithText("Retry").assertIsEnabled()
        composeRule.onNodeWithText("Not enough space").assertIsDisplayed()
    }

    @Test
    fun notInstalledEmitsInstallAction() {
        val actions = mutableListOf<InstallAction>()
        render(InstallState.NotInstalled, InstallActionHandler { action -> actions.add(action) })

        composeRule.onNodeWithText("Install").performClick()

        assertEquals(listOf(InstallAction.Install), actions)
    }

    @Test
    fun updateAvailableEmitsUpdateAction() {
        val actions = mutableListOf<InstallAction>()
        render(
            InstallState.UpdateAvailable(from = "v1.0.0", to = "v1.2.0"),
            InstallActionHandler { action -> actions.add(action) },
        )

        composeRule.onNodeWithText("Update").performClick()

        assertEquals(listOf(InstallAction.Update), actions)
    }
}

private val PARTLY_DOWNLOADED =
    downloadSizeOf(bytesDownloaded = 4_100_000L, bytesTotal = 12_100_000L)

private val UNKNOWN_TOTAL = downloadSizeOf(bytesDownloaded = 4_100_000L, bytesTotal = 0L)
