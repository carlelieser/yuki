package app.yuki.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
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

    private fun text(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun notInstalledRendersTheWebsiteAction() {
        setContent(ShizukuState.NotInstalled)

        composeRule.onNodeWithText(text(R.string.settings_card_not_installed_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_action_open_website)).assertIsDisplayed()
    }

    @Test
    fun notRunningRendersTheLaunchAction() {
        setContent(ShizukuState.NotRunning)

        composeRule.onNodeWithText(text(R.string.settings_card_not_running_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_action_launch_shizuku)).assertIsDisplayed()
    }

    @Test
    fun permissionRequiredRendersTheRequestAction() {
        setContent(ShizukuState.PermissionRequired)

        composeRule.onNodeWithText(text(R.string.settings_card_permission_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_action_request_permission)).assertIsDisplayed()
    }

    @Test
    fun tappingTheCardActionReportsItsKind() {
        val kinds = mutableListOf<ShizukuActionKind>()
        setContent(ShizukuState.NotRunning, onShizukuAction = kinds::add)

        composeRule.onNodeWithText(text(R.string.settings_action_launch_shizuku)).performClick()

        assertEquals(listOf(ShizukuActionKind.LaunchShizuku), kinds)
    }

    @Test
    fun aGrantedPermissionRendersAGrantedChip() {
        setContent(
            ShizukuState.Ready,
            permissionStatus = PermissionStatus.Granted,
            permissions = listOf(requiredPermission()),
        )

        composeRule.onNodeWithContentDescription(text(R.string.settings_permission_granted)).assertIsDisplayed()
    }

    @Test
    fun aDeniedPermissionRendersADeniedChip() {
        setContent(
            ShizukuState.NotInstalled,
            permissionStatus = PermissionStatus.Denied,
            permissions = listOf(requiredPermission()),
        )

        composeRule.onNodeWithContentDescription(text(R.string.settings_permission_denied)).assertIsDisplayed()
    }

    @Test
    fun aRequiredPermissionRendersTheRequiredBadge() {
        setContent(ShizukuState.Ready, permissions = listOf(requiredPermission()))

        composeRule.onNodeWithText(text(R.string.settings_permission_required)).assertIsDisplayed()
    }

    @Test
    fun anOptionalPermissionRendersNoIndicator() {
        setContent(ShizukuState.Ready, permissions = listOf(optionalPermission()))

        composeRule.onNodeWithText(text(R.string.settings_permission_required)).assertDoesNotExist()
    }

    @Test
    fun theInstallModeRowNamesTheActiveMode() {
        setContent(ShizukuState.Ready)
        scrollToInstallMode()

        composeRule.onAllNodesWithText(text(InstallMode.Shizuku.label)).onLast().assertIsDisplayed()
    }

    @Test
    fun theInstallModeSelectorOffersEveryMode() {
        setContent(ShizukuState.Ready)
        scrollToInstallMode()

        composeRule.onNodeWithTag(INSTALL_MODE_SELECTOR_TAG).performClick()

        InstallMode.entries.forEach { mode ->
            composeRule.onAllNodesWithText(text(mode.label)).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun choosingAModeReportsIt() {
        val modes = mutableListOf<InstallMode>()
        setContent(ShizukuState.Ready, onInstallModeChange = modes::add)
        scrollToInstallMode()

        composeRule.onNodeWithTag(INSTALL_MODE_SELECTOR_TAG).performClick()
        composeRule.onAllNodesWithText(text(InstallMode.System.label)).onLast().performClick()

        assertEquals(listOf(InstallMode.System), modes)
    }

    @Test
    fun theAppearanceSelectorOffersEveryMode() {
        setContent(ShizukuState.Ready)

        composeRule.onNodeWithTag(APPEARANCE_SELECTOR_TAG).performClick()

        AppearanceMode.entries.forEach { mode ->
            composeRule.onAllNodesWithText(text(mode.label)).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun choosingAnAppearanceReportsIt() {
        val modes = mutableListOf<AppearanceMode>()
        setContent(ShizukuState.Ready, onAppearanceChange = modes::add)

        composeRule.onNodeWithTag(APPEARANCE_SELECTOR_TAG).performClick()
        composeRule.onAllNodesWithText(text(AppearanceMode.Dark.label)).onLast().performClick()

        assertEquals(listOf(AppearanceMode.Dark), modes)
    }

    @Test
    fun theInstallSourceRowNamesTheActiveSource() {
        setContent(ShizukuState.Ready)
        scrollTo(INSTALL_SOURCE_SELECTOR_TAG)

        composeRule.onNodeWithText(text(R.string.settings_install_source_shell)).assertIsDisplayed()
    }

    @Test
    fun theInstallSourceRowNamesAChosenApp() {
        setContent(ShizukuState.Ready, installSource = InstallSourceName.App("Acme Store"))
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
        permissions: List<AppPermission> = devicePermissions(),
        onShizukuAction: (ShizukuActionKind) -> Unit = {},
        onInstallModeChange: (InstallMode) -> Unit = {},
        onAppearanceChange: (AppearanceMode) -> Unit = {},
        onInstallerPackageChange: (String) -> Unit = {},
        installSource: InstallSourceName = SHELL_PRESET,
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
                name = installSource,
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

private val SHELL_PRESET = InstallSourceName.Preset(R.string.settings_install_source_shell)

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
            onAutoUpdateCheckChange = {},
            onUpdateNotificationChange = {},
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
    devicePermissions().first { permission -> permission.isRequired }

private fun optionalPermission(): AppPermission =
    devicePermissions().first { permission -> !permission.isRequired }
