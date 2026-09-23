package app.yuki.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.UiState
import app.yuki.core.settings.api.SettingsContributor
import app.yuki.core.settings.api.SettingsGroup
import javax.inject.Inject

class SystemSettingsContributor @Inject constructor() : SettingsContributor {
    override val group = SettingsGroup.System

    @Composable
    override fun Content() {
        val viewModel: SettingsViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val chooser by viewModel.chooser.collectAsStateWithLifecycle()
        val destinations = rememberSystemDestinations()

        ResumeEffect(viewModel::onResume)

        val actions = remember(destinations, viewModel) {
            settingsActions(destinations = destinations, viewModel = viewModel)
        }

        SystemSettings(state = state, actions = actions)

        chooser?.let { open ->
            InstallSourceDialog(
                InstallSourcePrompt(
                    apps = open.apps,
                    iconOf = viewModel::installedAppIcon,
                    onSelect = { packageName ->
                        viewModel.onInstallerPackageChange(packageName)
                        viewModel.onDismissInstallerChooser()
                    },
                    onDismiss = viewModel::onDismissInstallerChooser,
                ),
            )
        }
    }
}

internal fun settingsActions(
    destinations: SystemDestinations,
    viewModel: SettingsViewModel,
): SettingsActions {
    val onRequestPermission = viewModel::onRequestShizukuPermission

    return SettingsActions(
        onShizukuAction = shizukuActionHandler(destinations, onRequestPermission),
        onPermissionClick = permissionClickHandler(destinations, onRequestPermission),
        preferences = PreferenceActions(
            onIncludePrereleasesChange = viewModel::onIncludePrereleasesChange,
            onInstallModeChange = viewModel::onInstallModeChange,
            onAppearanceChange = viewModel::onAppearanceChange,
            onDynamicColorChange = viewModel::onDynamicColorChange,
            onInstallerPackageChange = viewModel::onInstallerPackageChange,
            onChooseInstallerApp = viewModel::onChooseInstallerApp,
        ),
    )
}

@Composable
internal fun SystemSettings(
    state: UiState<SettingsContent>,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is UiState.Loading -> Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            YukiLoadingIndicator()
        }
        is UiState.Failure -> FailureState(reason = state.reason, modifier = modifier)
        is UiState.Success -> SettingsSections(
            content = state.data,
            actions = actions,
            modifier = modifier,
        )
    }
}

@Composable
private fun SettingsSections(
    content: SettingsContent,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
    ) {
        ShizukuSection(content = content, onShizukuAction = actions.onShizukuAction)

        PermissionsSection(
            rows = content.permissions,
            onPermissionClick = actions.onPermissionClick,
        )

        AppearanceSection(preferences = content.preferences, actions = actions.preferences)

        PreferencesSection(content = content, actions = actions.preferences)
    }
}
