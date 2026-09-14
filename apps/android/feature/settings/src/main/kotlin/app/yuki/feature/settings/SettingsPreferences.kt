package app.yuki.feature.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class InstallMode(val label: String) {
    Automatic("Silent"),
    AlwaysAsk("Always ask"),
}

enum class AppearanceMode(val label: String) {
    System("System default"),
    Light("Light"),
    Dark("Dark"),
}

data class YukiPreferences(
    val includePrereleases: Boolean,
    val installMode: InstallMode,
    val appearance: AppearanceMode,
    val isDynamicColorEnabled: Boolean,
) {
    companion object {
        val Defaults: YukiPreferences = YukiPreferences(
            includePrereleases = false,
            installMode = InstallMode.Automatic,
            appearance = AppearanceMode.System,
            isDynamicColorEnabled = true,
        )
    }
}

interface PreferenceStore {
    val preferences: Flow<YukiPreferences>

    suspend fun setIncludePrereleases(isEnabled: Boolean)

    suspend fun setInstallMode(mode: InstallMode)

    suspend fun setAppearance(mode: AppearanceMode)

    suspend fun setDynamicColorEnabled(isEnabled: Boolean)
}

private val IncludePrereleasesKey = booleanPreferencesKey("include_prereleases")
private val InstallModeKey = stringPreferencesKey("install_mode")
private val AppearanceKey = stringPreferencesKey("appearance")
private val DynamicColorKey = booleanPreferencesKey("dynamic_color")

internal fun decode(stored: Preferences): YukiPreferences = YukiPreferences(
    includePrereleases = stored[IncludePrereleasesKey]
        ?: YukiPreferences.Defaults.includePrereleases,
    installMode = decodeInstallMode(stored[InstallModeKey]),
    appearance = decodeAppearance(stored[AppearanceKey]),
    isDynamicColorEnabled = stored[DynamicColorKey]
        ?: YukiPreferences.Defaults.isDynamicColorEnabled,
)

private fun decodeInstallMode(raw: String?): InstallMode =
    InstallMode.entries.firstOrNull { mode -> mode.name == raw }
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
}
