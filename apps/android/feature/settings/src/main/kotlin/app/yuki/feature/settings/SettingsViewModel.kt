package app.yuki.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject internal constructor(
    private val dependencies: SettingsDependencies,
) : ViewModel() {
    private val permissionReads = MutableStateFlow(0)

    private val permissions: StateFlow<List<PermissionRow>> = permissionReads
        .map { dependencies.readPermissions() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, dependencies.readPermissions())

    val state: StateFlow<UiState<SettingsContent>> = combine(
        dependencies.shizuku.detail,
        permissions,
        dependencies.store.preferences,
    ) { shizuku, rows, preferences ->
        UiState.Success(
            SettingsContent(shizuku = shizuku, permissions = rows, preferences = preferences),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT), UiState.Loading)

    fun onResume() {
        dependencies.shizuku.refresh()
        permissionReads.value += 1
    }

    fun onRequestShizukuPermission() {
        dependencies.shizuku.requestPermission()
    }

    fun onIncludePrereleasesChange(isEnabled: Boolean) {
        viewModelScope.launch { dependencies.store.setIncludePrereleases(isEnabled) }
    }

    fun onInstallModeChange(mode: InstallMode) {
        viewModelScope.launch { dependencies.store.setInstallMode(mode) }
    }

    fun onAppearanceChange(mode: AppearanceMode) {
        viewModelScope.launch { dependencies.store.setAppearance(mode) }
    }

    fun onDynamicColorChange(isEnabled: Boolean) {
        viewModelScope.launch { dependencies.store.setDynamicColorEnabled(isEnabled) }
    }
}

internal class SettingsDependencies @Inject constructor(
    val shizuku: ShizukuSource,
    val store: PreferenceStore,
    private val reader: PermissionStatusReader,
) {
    fun readPermissions(): List<PermissionRow> = YUKI_PERMISSIONS.map { permission ->
        PermissionRow(permission = permission, status = reader.statusOf(permission))
    }
}

private const val SUBSCRIPTION_TIMEOUT = 5_000L
