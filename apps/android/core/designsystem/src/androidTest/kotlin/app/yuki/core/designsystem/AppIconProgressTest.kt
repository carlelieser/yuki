package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.APP_ICON_PROGRESS_TAG
import app.yuki.core.designsystem.component.INSTALLED_BADGE_TAG
import app.yuki.core.designsystem.component.ProductListItem
import app.yuki.core.designsystem.component.ProductListItemContent
import app.yuki.core.designsystem.theme.YukiTheme
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconProgressTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render(state: InstallState, isInstalled: Boolean = false) {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                ProductListItem(
                    content = ProductListItemContent(
                        title = "Example App",
                        supporting = "octocat",
                        iconUrl = null,
                        isInstalled = isInstalled,
                        installState = state,
                    ),
                )
            }
        }
    }

    @Test
    fun aDownloadOfUnknownSizeShowsProgress() {
        render(InstallState.Downloading(downloadSizeOf(0L, 0L)))

        composeRule.onNodeWithTag(APP_ICON_PROGRESS_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun aDownloadOfKnownSizeShowsProgress() {
        render(InstallState.Downloading(downloadSizeOf(512L, 1024L)))

        composeRule.onNodeWithTag(APP_ICON_PROGRESS_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun installingShowsProgress() {
        render(InstallState.Installing)

        composeRule.onNodeWithTag(APP_ICON_PROGRESS_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun anIdleRowShowsNoProgress() {
        render(InstallState.NotInstalled)

        composeRule.onNodeWithTag(APP_ICON_PROGRESS_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun anInstalledRowShowsNoProgress() {
        render(InstallState.Installed("1.0.0"), isInstalled = true)

        composeRule.onNodeWithTag(APP_ICON_PROGRESS_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(INSTALLED_BADGE_TAG).assertIsDisplayed()
    }

    @Test
    fun aDownloadingRowHidesTheInstalledBadge() {
        render(InstallState.Downloading(downloadSizeOf(512L, 1024L)), isInstalled = true)

        composeRule.onNodeWithTag(INSTALLED_BADGE_TAG).assertDoesNotExist()
    }
}
