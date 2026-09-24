package app.yuki.feature.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.yuki.core.shizuku.SHELL_INSTALLER_PACKAGE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private val InstallModeKey = stringPreferencesKey("install_mode")

@OptIn(ExperimentalCoroutinesApi::class)
class DataStorePreferenceStoreTest {
    @get:Rule
    val folder: TemporaryFolder = TemporaryFolder()

    @Test
    fun anUntouchedStoreReadsBackTheDefaults() = runTest {
        withStore { store ->
            assertEquals(YukiPreferences.Defaults, store.preferences.first())
        }
    }

    @Test
    fun includePrereleasesSurvivesAWriteAndReadBack() = runTest {
        withStore { store ->
            store.setIncludePrereleases(true)
            assertTrue(store.preferences.first().includePrereleases)

            store.setIncludePrereleases(false)
            assertFalse(store.preferences.first().includePrereleases)
        }
    }

    @Test
    fun installModeSurvivesAWriteAndReadBack() = runTest {
        withStore { store ->
            store.setInstallMode(InstallMode.System)
            assertEquals(InstallMode.System, store.preferences.first().installMode)

            store.setInstallMode(InstallMode.Shizuku)
            assertEquals(InstallMode.Shizuku, store.preferences.first().installMode)
        }
    }

    @Test
    fun legacyInstallModeNamesStillDecode() = runTest {
        val file = folder.newFile("legacy.preferences_pb").also(java.io.File::delete)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val raw: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) { file }

        try {
            val store = DataStorePreferenceStore(raw)

            raw.edit { stored -> stored[InstallModeKey] = "AlwaysAsk" }
            assertEquals(InstallMode.System, store.preferences.first().installMode)

            raw.edit { stored -> stored[InstallModeKey] = "Automatic" }
            assertEquals(InstallMode.Shizuku, store.preferences.first().installMode)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun dynamicColorSurvivesAWriteAndReadBack() = runTest {
        withStore { store ->
            store.setDynamicColorEnabled(false)
            assertFalse(store.preferences.first().isDynamicColorEnabled)

            store.setDynamicColorEnabled(true)
            assertTrue(store.preferences.first().isDynamicColorEnabled)
        }
    }

    @Test
    fun installerPackageSurvivesAWriteAndReadBack() = runTest {
        withStore { store ->
            store.setInstallerPackage("com.android.vending")
            assertEquals("com.android.vending", store.preferences.first().installerPackage)

            store.setInstallerPackage(SHELL_INSTALLER_PACKAGE)
            assertEquals(SHELL_INSTALLER_PACKAGE, store.preferences.first().installerPackage)
        }
    }

    @Test
    fun aBlankStoredInstallerPackageReadsBackAsShell() = runTest {
        withStore { store ->
            store.setInstallerPackage("   ")

            assertEquals(SHELL_INSTALLER_PACKAGE, store.preferences.first().installerPackage)
        }
    }

    @Test
    fun eachPreferenceIsStoredUnderItsOwnKey() = runTest {
        withStore { store ->
            store.setIncludePrereleases(true)
            store.setInstallMode(InstallMode.System)
            store.setAppearance(AppearanceMode.Dark)
            store.setDynamicColorEnabled(false)
            store.setInstallerPackage("com.android.vending")

            assertEquals(
                YukiPreferences(
                    includePrereleases = true,
                    installMode = InstallMode.System,
                    appearance = AppearanceMode.Dark,
                    isDynamicColorEnabled = false,
                    installerPackage = "com.android.vending",
                ),
                store.preferences.first(),
            )
        }
    }

    @Test
    fun aWrittenValueIsStillThereForANewStoreOverTheSameFile() = runTest {
        val file = folder.newFile("round-trip.preferences_pb").also(java.io.File::delete)

        withStoreAt(file) { store -> store.setIncludePrereleases(true) }
        withStoreAt(file) { store ->
            assertTrue(store.preferences.first().includePrereleases)
        }
    }

    private suspend fun withStore(block: suspend (PreferenceStore) -> Unit) {
        val file = folder.newFile("settings.preferences_pb").also(java.io.File::delete)
        withStoreAt(file, block)
    }

    private suspend fun withStoreAt(
        file: java.io.File,
        block: suspend (PreferenceStore) -> Unit,
    ) {
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val preferences: DataStore<Preferences> =
            PreferenceDataStoreFactory.create(scope = scope) { file }

        try {
            block(DataStorePreferenceStore(preferences))
        } finally {
            scope.cancel()
        }
    }
}
