package app.yuki.feature.settings

import app.yuki.core.shizuku.SHELL_INSTALLER_PACKAGE
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceDecodingTest {
    @Test
    fun defaultsFavourStableReleasesShizukuInstallsAndDynamicColor() {
        assertFalse(YukiPreferences.Defaults.includePrereleases)
        assertEquals(InstallMode.Shizuku, YukiPreferences.Defaults.installMode)
        assertEquals(AppearanceMode.System, YukiPreferences.Defaults.appearance)
        assertTrue(YukiPreferences.Defaults.isDynamicColorEnabled)
        assertEquals(SHELL_INSTALLER_PACKAGE, YukiPreferences.Defaults.installerPackage)
    }



    @Test
    fun theStoredNameIsIndependentOfTheDisplayedLabel() {
        assertEquals("Shizuku", InstallMode.Shizuku.name)
        assertEquals("System", InstallMode.System.name)
    }

    @Test
    fun theReaderSurfacesEachStoredPreference() = runTest {
        val store = FakePreferenceStore()
        val reader = StoreBackedPreferenceReader(store)

        store.setIncludePrereleases(true)
        store.setInstallMode(InstallMode.System)
        store.setAppearance(AppearanceMode.Dark)
        store.setDynamicColorEnabled(false)
        store.setInstallerPackage("com.android.vending")

        assertTrue(reader.includePrereleases().first())
        assertEquals(InstallMode.System, reader.installMode().first())
        assertEquals(AppearanceMode.Dark, reader.appearance().first())
        assertFalse(reader.isDynamicColorEnabled().first())
        assertEquals("com.android.vending", reader.installerPackage().first())
    }

    @Test
    fun theReaderStartsFromTheDefaults() = runTest {
        val reader = StoreBackedPreferenceReader(FakePreferenceStore())

        assertFalse(reader.includePrereleases().first())
        assertEquals(InstallMode.Shizuku, reader.installMode().first())
        assertEquals(AppearanceMode.System, reader.appearance().first())
        assertTrue(reader.isDynamicColorEnabled().first())
        assertEquals(SHELL_INSTALLER_PACKAGE, reader.installerPackage().first())
    }
}
