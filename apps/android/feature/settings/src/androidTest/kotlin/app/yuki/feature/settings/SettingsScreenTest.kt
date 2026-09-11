package app.yuki.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.yuki.core.model.UiState
import app.yuki.core.shizuku.ShizukuDetail
import app.yuki.core.shizuku.ShizukuMode
import app.yuki.core.shizuku.ShizukuState
import app.yuki.core.shizuku.UNKNOWN_API_VERSION
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun notInstalledRendersTheWebsiteAction() {
        setContent(ShizukuState.NotInstalled)

        composeRule.onNodeWithText(CARD_NOT_INSTALLED_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(ACTION_OPEN_WEBSITE).assertIsDisplayed()
    }

    @Test
    fun notRunningRendersTheLaunchAction() {
        setContent(ShizukuState.NotRunning)

        composeRule.onNodeWithText(CARD_NOT_RUNNING_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(ACTION_LAUNCH_SHIZUKU).assertIsDisplayed()
    }

    @Test
    fun permissionRequiredRendersTheRequestAction() {
        setContent(ShizukuState.PermissionRequired)

        composeRule.onNodeWithText(CARD_PERMISSION_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(ACTION_REQUEST_PERMISSION).assertIsDisplayed()
    }

    @Test
    fun tappingTheCardActionReportsItsKind() {
        val kinds = mutableListOf<ShizukuActionKind>()
        setContent(ShizukuState.NotRunning, onShizukuAction = kinds::add)

        composeRule.onNodeWithText(ACTION_LAUNCH_SHIZUKU).performClick()

        assertEquals(listOf(ShizukuActionKind.LaunchShizuku), kinds)
    }

    @Test
    fun aGrantedPermissionRendersAGrantedChip() {
        setContent(ShizukuState.Ready, permissionStatus = PermissionStatus.Granted)

        composeRule.onNodeWithText(CHIP_GRANTED).assertIsDisplayed()
    }

    @Test
    fun aDeniedPermissionRendersADeniedChip() {
        setContent(ShizukuState.NotInstalled, permissionStatus = PermissionStatus.Denied)

        composeRule.onNodeWithText(CHIP_DENIED).assertIsDisplayed()
    }

    @Test
    fun aRequiredPermissionRendersTheRequiredBadge() {
        setContent(ShizukuState.Ready, permissions = listOf(requiredPermission()))

        composeRule.onNodeWithText(REQUIRED_LABEL).assertIsDisplayed()
    }

    @Test
    fun anOptionalPermissionRendersNoIndicator() {
        setContent(ShizukuState.Ready, permissions = listOf(optionalPermission()))

        composeRule.onNodeWithText(REQUIRED_LABEL).assertDoesNotExist()
    }

    private fun setContent(
        state: ShizukuState,
        permissionStatus: PermissionStatus = PermissionStatus.Denied,
        permissions: List<AppPermission> = YUKI_PERMISSIONS,
        onShizukuAction: (ShizukuActionKind) -> Unit = {},
    ) {
        val content = SettingsContent(
            shizuku = detailFor(state),
            permissions = permissions.map { permission ->
                PermissionRow(permission = permission, status = permissionStatus)
            },
            preferences = YukiPreferences.Defaults,
        )

        composeRule.setContent {
            SettingsContentScreen(
                state = UiState.Success(content),
                actions = actionsWith(onShizukuAction),
            )
        }
    }
}

private fun actionsWith(onShizukuAction: (ShizukuActionKind) -> Unit): SettingsActions =
    SettingsActions(
        onShizukuAction = onShizukuAction,
        onPermissionClick = {},
        preferences = PreferenceActions(
            onIncludePrereleasesChange = {},
            onInstallModeChange = {},
            onDynamicColorChange = {},
        ),
    )

private fun detailFor(state: ShizukuState): ShizukuDetail = when (state) {
    ShizukuState.Ready -> ShizukuDetail(state, ShizukuMode.AdbShell, apiVersion = 13)
    else -> ShizukuDetail(state, ShizukuMode.Unknown, UNKNOWN_API_VERSION)
}

private fun requiredPermission(): AppPermission =
    YUKI_PERMISSIONS.first { permission -> permission.isRequired }

private fun optionalPermission(): AppPermission =
    YUKI_PERMISSIONS.first { permission -> !permission.isRequired }
