package app.yuki.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.UiState

const val SETTINGS_LIST_TAG = "settingsList"

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val destinations = rememberSystemDestinations()

    ResumeEffect(viewModel::onResume)

    val actions = remember(destinations, viewModel) {
        settingsActions(destinations = destinations, viewModel = viewModel)
    }

    SettingsContentScreen(state = state, actions = actions, modifier = modifier)
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
        ),
    )
}

@Composable
internal fun SettingsContentScreen(
    state: UiState<SettingsContent>,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when (state) {
            is UiState.Loading -> SettingsLoading()
            is UiState.Failure -> FailureState(
                reason = state.reason,
                modifier = Modifier.padding(YukiSpacing.Large),
            )
            is UiState.Success -> SettingsList(content = state.data, actions = actions)
        }
    }
}

@Composable
private fun SettingsLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        YukiLoadingIndicator()
    }
}

@Composable
private fun SettingsList(content: SettingsContent, actions: SettingsActions) {
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(SETTINGS_LIST_TAG)) {
        shizukuSection(content = content, onShizukuAction = actions.onShizukuAction)
        permissionsSection(
            rows = content.permissions,
            onPermissionClick = actions.onPermissionClick,
        )
        preferencesSection(preferences = content.preferences, actions = actions.preferences)
    }
}
