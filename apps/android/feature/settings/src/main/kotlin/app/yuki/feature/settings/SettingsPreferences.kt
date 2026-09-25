package app.yuki.feature.settings

import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.yuki.core.shizuku.SHELL_INSTALLER_PACKAGE
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class InstallMode(@StringRes val label: Int) {
    Shizuku(R.string.settings_install_mode_shizuku),
    System(R.string.settings_install_mode_system),
}

enum class AppearanceMode(@StringRes val label: Int) {
    System(R.string.settings_appearance_system),
    Light(R.string.settings_appearance_light),
    Dark(R.string.settings_appearance_dark),
}

data class YukiPreferences(
    val includePrereleases: Boolean,
    val installMode: InstallMode,
    val appearance: AppearanceMode,
    val isDynamicColorEnabled: Boolean,
    val installerPackage: String,
) {
    companion object {
        val Defaults: YukiPreferences = YukiPreferences(
            includePrereleases = false,
            installMode = InstallMode.Shizuku,
            appearance = AppearanceMode.System,
            isDynamicColorEnabled = true,
            installerPackage = SHELL_INSTALLER_PACKAGE,
        )
    }
}

interface PreferenceStore {
    val preferences: Flow<YukiPreferences>

    suspend fun setIncludePrereleases(isEnabled: Boolean)

    suspend fun setInstallMode(mode: InstallMode)

    suspend fun setAppearance(mode: AppearanceMode)

    suspend fun setDynamicColorEnabled(isEnabled: Boolean)

    suspend fun setInstallerPackage(packageName: String)
}

private val IncludePrereleasesKey = booleanPreferencesKey("include_prereleases")
private val InstallModeKey = stringPreferencesKey("install_mode")
private val AppearanceKey = stringPreferencesKey("appearance")
private val DynamicColorKey = booleanPreferencesKey("dynamic_color")
private val InstallerPackageKey = stringPreferencesKey("installer_package")

internal fun decode(stored: Preferences): YukiPreferences = YukiPreferences(
    includePrereleases = stored[IncludePrereleasesKey]
        ?: YukiPreferences.Defaults.includePrereleases,
    installMode = decodeInstallMode(stored[InstallModeKey]),
    appearance = decodeAppearance(stored[AppearanceKey]),
    isDynamicColorEnabled = stored[DynamicColorKey]
        ?: YukiPreferences.Defaults.isDynamicColorEnabled,
    installerPackage = decodeInstallerPackage(stored[InstallerPackageKey]),
)

private fun decodeInstallerPackage(raw: String?): String =
    raw?.takeIf(String::isNotBlank) ?: YukiPreferences.Defaults.installerPackage

private val LegacyInstallModes: Map<String, InstallMode> = mapOf(
    "Automatic" to InstallMode.Shizuku,
    "AlwaysAsk" to InstallMode.System,
)

private fun decodeInstallMode(raw: String?): InstallMode =
    InstallMode.entries.firstOrNull { mode -> mode.name == raw }
        ?: LegacyInstallModes[raw]
        ?: YukiPreferences.Defaults.installMode

private fun decodeAppearance(raw: String?): AppearanceMode =
    AppearanceMode.entries.firstOrNull { mode -> mode.name == raw }
        ?: YukiPreferences.Defaults.appearance

@Singleton
internal class DataStorePreferenceStore @Inject constructor(
    @param:SettingsPreferences private val store: DataStore<Preferences>,
) : PreferenceStore {
    override val preferences: Flow<YukiPreferences> = store.data.map(::decode)

    override suspend fun setIncludePrereleases(isEnabled: Boolean) {
        store.edit { stored -> stored[IncludePrereleasesKey] = isEnabled }
    }

    override suspend fun setInstallMode(mode: InstallMode) {
        store.edit { stored -> stored[InstallModeKey] = mode.name }
    }

    override suspend fun setAppearance(mode: AppearanceMode) {
        store.edit { stored -> stored[AppearanceKey] = mode.name }
    }

    override suspend fun setDynamicColorEnabled(isEnabled: Boolean) {
        store.edit { stored -> stored[DynamicColorKey] = isEnabled }
    }

    override suspend fun setInstallerPackage(packageName: String) {
        store.edit { stored -> stored[InstallerPackageKey] = packageName }
    }
}
