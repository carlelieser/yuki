package app.yuki.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
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
        setContent(
            ShizukuState.Ready,
            permissionStatus = PermissionStatus.Granted,
            permissions = listOf(requiredPermission()),
        )

        composeRule.onNodeWithContentDescription(STATUS_GRANTED).assertIsDisplayed()
    }

    @Test
    fun aDeniedPermissionRendersADeniedChip() {
        setContent(
            ShizukuState.NotInstalled,
            permissionStatus = PermissionStatus.Denied,
            permissions = listOf(requiredPermission()),
        )

        composeRule.onNodeWithContentDescription(STATUS_DENIED).assertIsDisplayed()
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

    @Test
    fun theInstallModeRowNamesTheActiveMode() {
        setContent(ShizukuState.Ready)
        scrollToInstallMode()

        composeRule.onAllNodesWithText(InstallMode.Shizuku.label).onLast().assertIsDisplayed()
    }

    @Test
    fun theInstallModeSelectorOffersEveryMode() {
        setContent(ShizukuState.Ready)
        scrollToInstallMode()

        composeRule.onNodeWithTag(INSTALL_MODE_SELECTOR_TAG).performClick()

        InstallMode.entries.forEach { mode ->
            composeRule.onAllNodesWithText(mode.label).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun choosingAModeReportsIt() {
        val modes = mutableListOf<InstallMode>()
        setContent(ShizukuState.Ready, onInstallModeChange = modes::add)
        scrollToInstallMode()

        composeRule.onNodeWithTag(INSTALL_MODE_SELECTOR_TAG).performClick()
        composeRule.onAllNodesWithText(InstallMode.System.label).onLast().performClick()

        assertEquals(listOf(InstallMode.System), modes)
    }

    @Test
    fun theAppearanceSelectorOffersEveryMode() {
        setContent(ShizukuState.Ready)

        composeRule.onNodeWithTag(APPEARANCE_SELECTOR_TAG).performClick()

        AppearanceMode.entries.forEach { mode ->
            composeRule.onAllNodesWithText(mode.label).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun choosingAnAppearanceReportsIt() {
        val modes = mutableListOf<AppearanceMode>()
        setContent(ShizukuState.Ready, onAppearanceChange = modes::add)

        composeRule.onNodeWithTag(APPEARANCE_SELECTOR_TAG).performClick()
        composeRule.onAllNodesWithText(AppearanceMode.Dark.label).onLast().performClick()

        assertEquals(listOf(AppearanceMode.Dark), modes)
    }

    @Test
    fun theInstallSourceRowNamesTheActiveSource() {
        setContent(ShizukuState.Ready)
        scrollTo(INSTALL_SOURCE_SELECTOR_TAG)

        composeRule.onNodeWithText(SHELL_PRESET_LABEL).assertIsDisplayed()
    }

    @Test
    fun theInstallSourceRowNamesAChosenApp() {
        setContent(ShizukuState.Ready, installSourceLabel = "Acme Store")
        scrollTo(INSTALL_SOURCE_SELECTOR_TAG)

        composeRule.onNodeWithText("Acme Store").assertIsDisplayed()
    }

    @Test
    fun theInstallSourceRowIsDisabledWhenTheSystemInstallerIsPreferred() {
        setContent(ShizukuState.Ready, isInstallSourceEnabled = false)
        scrollTo(INSTALL_SOURCE_SELECTOR_TAG)

        composeRule.onNodeWithTag(INSTALL_SOURCE_SELECTOR_TAG).assertIsNotEnabled()
    }

    @Test
    fun openingTheChooserListsInstalledApps() {
        setContent(ShizukuState.Ready, chooser = InstallSourceChooser(INSTALLED_APPS))

        composeRule.onNodeWithTag(INSTALL_SOURCE_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Acme Store").assertIsDisplayed()
        composeRule.onNodeWithText("net.example.files").assertIsDisplayed()
    }

    @Test
    fun choosingAnAppReportsItsPackage() {
        val packages = mutableListOf<String>()
        setContent(
            ShizukuState.Ready,
            chooser = InstallSourceChooser(INSTALLED_APPS),
            onInstallerPackageChange = packages::add,
        )

        composeRule.onNodeWithText("Acme Store").performClick()

        assertEquals(listOf("com.acme.store"), packages)
    }

    private fun scrollTo(tag: String) {
        composeRule.onNodeWithTag(SETTINGS_LIST_TAG).performScrollToNode(hasTestTag(tag))
    }

    private fun scrollToInstallMode() {
        composeRule.onNodeWithTag(SETTINGS_LIST_TAG)
            .performScrollToNode(hasTestTag(INSTALL_MODE_SELECTOR_TAG))
    }

    private fun setContent(
        state: ShizukuState,
        permissionStatus: PermissionStatus = PermissionStatus.Denied,
        permissions: List<AppPermission> = YUKI_PERMISSIONS,
        onShizukuAction: (ShizukuActionKind) -> Unit = {},
        onInstallModeChange: (InstallMode) -> Unit = {},
        onAppearanceChange: (AppearanceMode) -> Unit = {},
        onInstallerPackageChange: (String) -> Unit = {},
        installSourceLabel: String = SHELL_PRESET_LABEL,
        isInstallSourceEnabled: Boolean = true,
        chooser: InstallSourceChooser? = null,
    ) {
        val content = SettingsContent(
            shizuku = detailFor(state),
            permissions = permissions.map { permission ->
                PermissionRow(permission = permission, status = permissionStatus)
            },
            preferences = YukiPreferences.Defaults,
            installSource = InstallSourceSelection(
                label = installSourceLabel,
                isPlayStoreInstalled = true,
                isEnabled = isInstallSourceEnabled,
            ),
        )
        val actions = actionsWith(onShizukuAction, onInstallModeChange, onAppearanceChange)
            .withInstallerPackageChange(onInstallerPackageChange)

        composeRule.setContent {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .testTag(SETTINGS_LIST_TAG),
            ) {
                SystemSettings(state = UiState.Success(content), actions = actions)
            }

            if (chooser == null) return@setContent

            InstallSourceDialog(
                InstallSourcePrompt(
                    apps = chooser.apps,
                    iconOf = { null },
                    onSelect = onInstallerPackageChange,
                    onDismiss = {},
                ),
            )
        }
    }
}

private const val SHELL_PRESET_LABEL = "Shell"

private val INSTALLED_APPS = listOf(
    InstalledApp(packageName = "com.acme.store", label = "Acme Store"),
    InstalledApp(packageName = "net.example.files", label = "Files"),
)

private fun SettingsActions.withInstallerPackageChange(
    onInstallerPackageChange: (String) -> Unit,
): SettingsActions = copy(
    preferences = preferences.copy(onInstallerPackageChange = onInstallerPackageChange),
)

private fun actionsWith(
    onShizukuAction: (ShizukuActionKind) -> Unit,
    onInstallModeChange: (InstallMode) -> Unit = {},
    onAppearanceChange: (AppearanceMode) -> Unit = {},
): SettingsActions =
    SettingsActions(
        onShizukuAction = onShizukuAction,
        onPermissionClick = {},
        preferences = PreferenceActions(
            onIncludePrereleasesChange = {},
            onInstallModeChange = onInstallModeChange,
            onAppearanceChange = onAppearanceChange,
            onDynamicColorChange = {},
            onInstallerPackageChange = {},
            onChooseInstallerApp = {},
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
