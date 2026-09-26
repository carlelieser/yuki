package app.yuki.feature.settings

import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject internal constructor(
    private val dependencies: SettingsDependencies,
) : ViewModel() {
    private val permissionReads = MutableStateFlow(0)
    private val openChooser = MutableStateFlow<InstallSourceChooser?>(null)

    private val permissions: StateFlow<List<PermissionRow>> = permissionReads
        .map { dependencies.readPermissions() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, dependencies.readPermissions())

    private val installChoice: Flow<InstallChoice> = dependencies.store.preferences
        .map { preferences -> InstallChoice(preferences.installerPackage, preferences.installMode) }
        .distinctUntilChanged()

    private val installSource: Flow<InstallSourceSelection> =
        combine(installChoice, permissionReads) { choice, _ ->
            dependencies.installSourceOf(choice)
        }

    val chooser: StateFlow<InstallSourceChooser?> = openChooser

    val state: StateFlow<UiState<SettingsContent>> = combine(
        dependencies.shizuku.detail,
        permissions,
        dependencies.store.preferences,
        installSource,
    ) { shizuku, rows, preferences, source ->
        UiState.Success(
            SettingsContent(
                shizuku = shizuku,
                permissions = rows,
                preferences = preferences,
                installSource = source,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT), UiState.Loading)

    fun onResume() {
        dependencies.shizuku.refresh()
        permissionReads.value += 1
    }

    fun onRequestShizukuPermission() {
        dependencies.shizuku.requestPermission()
    }

    fun onAutoUpdateCheckChange(isEnabled: Boolean) {
        viewModelScope.launch { dependencies.store.setAutoUpdateCheckEnabled(isEnabled) }
    }

    fun onUpdateNotificationChange(isEnabled: Boolean) {
        viewModelScope.launch { dependencies.store.setUpdateNotificationEnabled(isEnabled) }
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

    fun onInstallerPackageChange(packageName: String) {
        viewModelScope.launch { dependencies.store.setInstallerPackage(packageName) }
    }

    fun onChooseInstallerApp() {
        openChooser.value = InstallSourceChooser(apps = null)
        viewModelScope.launch {
            val apps = dependencies.apps.read()
            if (openChooser.value != null) openChooser.value = InstallSourceChooser(apps)
        }
    }

    fun onDismissInstallerChooser() {
        openChooser.value = null
    }

    suspend fun installedAppIcon(packageName: String): Drawable? =
        dependencies.apps.iconOf(packageName)
}

internal class SettingsDependencies @Inject constructor(
    val shizuku: ShizukuSource,
    val store: PreferenceStore,
    private val readers: SettingsReaders,
) {
    val apps: InstalledAppsReader get() = readers.apps

    fun readPermissions(): List<PermissionRow> = devicePermissions().map { permission ->
        PermissionRow(permission = permission, status = readers.permissions.statusOf(permission))
    }

    suspend fun installSourceOf(choice: InstallChoice): InstallSourceSelection =
        InstallSourceSelection(
            name = installSourceName(
                choice.installerPackage,
                readers.apps.labelOf(choice.installerPackage),
            ),
            isPlayStoreInstalled = readers.apps.labelOf(PLAY_STORE_PACKAGE) != null,
            isEnabled = installSourceApplies(choice.mode),
        )
}

internal data class InstallChoice(val installerPackage: String, val mode: InstallMode)

internal class SettingsReaders @Inject constructor(
    val permissions: PermissionStatusReader,
    val apps: InstalledAppsReader,
)

private const val SUBSCRIPTION_TIMEOUT = 5_000L
