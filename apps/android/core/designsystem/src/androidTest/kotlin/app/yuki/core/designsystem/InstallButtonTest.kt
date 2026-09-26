package app.yuki.core.designsystem

import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.INSTALL_PROGRESS_TAG
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.designsystem.component.InstallButton
import app.yuki.core.designsystem.component.InstallProgressPosition
import app.yuki.core.designsystem.component.InstallProgressShape
import app.yuki.core.designsystem.component.formatPercent
import app.yuki.core.designsystem.theme.YukiTheme
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallButtonTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(@StringRes id: Int, vararg args: Any): String =
        composeRule.activity.getString(id, *args)

    private fun partlyDownloadedPercent(): String =
        formatPercent(requireNotNull(PARTLY_DOWNLOADED.fraction), Locale.getDefault())

    private fun bytes(count: Long): String = Formatter.formatShortFileSize(composeRule.activity, count)

    private fun partlyDownloadedLabel(): String = text(
        R.string.designsystem_download_size,
        bytes(PARTLY_DOWNLOADED.bytesDownloaded),
        bytes(checkNotNull(PARTLY_DOWNLOADED.bytesTotal)),
    )

    private fun render(state: InstallState, onAction: InstallActionHandler = InstallActionHandler { }) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                InstallButton(state = state, onAction = onAction)
            }
        }
    }

    private fun renderUninstallable(
        state: InstallState,
        onAction: InstallActionHandler = InstallActionHandler { },
    ) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                InstallButton(state = state, onAction = onAction, canUninstall = true)
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
    fun aDownloadWithoutAProgressPositionShowsOnlyTheCancelButton() {
        renderCircular(
            state = InstallState.Downloading(PARTLY_DOWNLOADED),
            position = InstallProgressPosition.None,
        )

        composeRule.onNodeWithText(text(R.string.designsystem_install_cancel)).assertIsEnabled()
        composeRule.onNodeWithText(partlyDownloadedLabel()).assertDoesNotExist()
        composeRule.onNodeWithTag(INSTALL_PROGRESS_TAG).assertDoesNotExist()
    }

    @Test
    fun aCircularDownloadDropsTheSizeLabel() {
        renderCircular(
            state = InstallState.Downloading(PARTLY_DOWNLOADED),
            position = InstallProgressPosition.Leading,
        )

        composeRule.onNodeWithText(partlyDownloadedLabel()).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.designsystem_install_cancel)).assertIsEnabled()
    }

    @Test
    fun aCircularDownloadStillDescribesProgress() {
        renderCircular(
            state = InstallState.Downloading(PARTLY_DOWNLOADED),
            position = InstallProgressPosition.Trailing,
        )

        composeRule
            .onNodeWithContentDescription(text(R.string.designsystem_install_downloading_progress, partlyDownloadedPercent(), partlyDownloadedLabel()))
            .assertExists()
    }

    @Test
    fun notInstalledOffersInstall() {
        render(InstallState.NotInstalled)

        composeRule.onNodeWithText(text(R.string.designsystem_install)).assertIsEnabled()
    }

    @Test
    fun downloadingOffersCancel() {
        render(InstallState.Downloading(PARTLY_DOWNLOADED))

        composeRule.onNodeWithText(text(R.string.designsystem_install_cancel)).assertIsEnabled()
    }

    @Test
    fun downloadingShowsTheTransferredBytes() {
        render(InstallState.Downloading(PARTLY_DOWNLOADED))

        composeRule.onNodeWithText(partlyDownloadedLabel()).assertExists()
    }

    @Test
    fun aDownloadWithAnUnknownTotalShowsNoByteCountsAtAll() {
        render(InstallState.Downloading(UNKNOWN_TOTAL))

        composeRule.onNodeWithText(bytes(PARTLY_DOWNLOADED.bytesDownloaded), substring = true).assertDoesNotExist()
    }

    @Test
    fun pendingUserActionCanStillBeCancelled() {
        val actions = mutableListOf<InstallAction>()
        render(InstallState.PendingUserAction, InstallActionHandler { action -> actions += action })

        composeRule.onNodeWithText(text(R.string.designsystem_install_pending)).assertIsEnabled().performClick()

        assertEquals(listOf(InstallAction.Cancel), actions)
    }

    @Test
    fun installingShowsItsLabel() {
        render(InstallState.Installing)

        composeRule.onNodeWithText(text(R.string.designsystem_install_installing)).assertExists()
    }

    @Test
    fun installingCannotBeCancelled() {
        val actions = mutableListOf<InstallAction>()
        render(InstallState.Installing, InstallActionHandler { action -> actions += action })

        composeRule.onNodeWithText(text(R.string.designsystem_install_installing)).assertIsNotEnabled().performClick()

        assertEquals(emptyList<InstallAction>(), actions)
    }

    @Test
    fun installedOffersOpen() {
        render(InstallState.Installed(versionTag = "v1.2.0"))

        composeRule.onNodeWithText(text(R.string.designsystem_install_open)).assertIsEnabled()
    }

    @Test
    fun installedOffersUninstallAlongsideOpenWhenAllowed() {
        renderUninstallable(InstallState.Installed(versionTag = "v1.2.0"))

        composeRule.onNodeWithText(text(R.string.designsystem_install_uninstall)).assertIsEnabled()
        composeRule.onNodeWithText(text(R.string.designsystem_install_open)).assertIsEnabled()
    }

    @Test
    fun installedHidesUninstallByDefault() {
        render(InstallState.Installed(versionTag = "v1.2.0"))

        composeRule.onNodeWithText(text(R.string.designsystem_install_uninstall)).assertDoesNotExist()
    }

    @Test
    fun uninstallIsNotOfferedBeforeAnAppIsInstalled() {
        renderUninstallable(InstallState.NotInstalled)

        composeRule.onNodeWithText(text(R.string.designsystem_install_uninstall)).assertDoesNotExist()
    }

    @Test
    fun installedEmitsUninstallAction() {
        val actions = mutableListOf<InstallAction>()
        renderUninstallable(
            state = InstallState.Installed(versionTag = "v1.2.0"),
            onAction = InstallActionHandler { action -> actions.add(action) },
        )

        composeRule.onNodeWithText(text(R.string.designsystem_install_uninstall)).performClick()

        assertEquals(listOf(InstallAction.Uninstall), actions)
    }

    @Test
    fun updateAvailableOffersUpdate() {
        render(InstallState.UpdateAvailable(from = "v1.0.0", to = "v1.2.0"))

        composeRule.onNodeWithText(text(R.string.designsystem_install_update)).assertIsEnabled()
    }

    @Test
    fun failedOffersRetry() {
        render(InstallState.Failed(reason = InstallFailure.InsufficientStorage))

        composeRule.onNodeWithText(text(R.string.designsystem_install_retry)).assertIsEnabled()
    }

    @Test
    fun notInstalledEmitsInstallAction() {
        val actions = mutableListOf<InstallAction>()
        render(InstallState.NotInstalled, InstallActionHandler { action -> actions.add(action) })

        composeRule.onNodeWithText(text(R.string.designsystem_install)).performClick()

        assertEquals(listOf(InstallAction.Install), actions)
    }

    @Test
    fun updateAvailableEmitsUpdateAction() {
        val actions = mutableListOf<InstallAction>()
        render(
            InstallState.UpdateAvailable(from = "v1.0.0", to = "v1.2.0"),
            InstallActionHandler { action -> actions.add(action) },
        )

        composeRule.onNodeWithText(text(R.string.designsystem_install_update)).performClick()

        assertEquals(listOf(InstallAction.Update), actions)
    }
}

private val PARTLY_DOWNLOADED =
    downloadSizeOf(bytesDownloaded = 4_100_000L, bytesTotal = 12_100_000L)

private val UNKNOWN_TOTAL = downloadSizeOf(bytesDownloaded = 4_100_000L, bytesTotal = 0L)
