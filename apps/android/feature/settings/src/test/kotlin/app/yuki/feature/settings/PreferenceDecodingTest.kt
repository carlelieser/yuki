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
    fun defaultsFavourStableReleasesAutomaticInstallsAndDynamicColor() {
        assertFalse(YukiPreferences.Defaults.includePrereleases)
        assertEquals(InstallMode.Automatic, YukiPreferences.Defaults.installMode)
        assertEquals(AppearanceMode.System, YukiPreferences.Defaults.appearance)
        assertTrue(YukiPreferences.Defaults.isDynamicColorEnabled)
        assertEquals(SHELL_INSTALLER_PACKAGE, YukiPreferences.Defaults.installerPackage)
    }

    @Test
    fun eachAppearanceModeOffersAName() {
        assertEquals("System default", AppearanceMode.System.label)
        assertEquals("Light", AppearanceMode.Light.label)
        assertEquals("Dark", AppearanceMode.Dark.label)
    }

    @Test
    fun eachInstallModeOffersAName() {
        assertEquals("Silent", InstallMode.Automatic.label)
        assertEquals("Always ask", InstallMode.AlwaysAsk.label)
    }

    @Test
    fun theStoredNameIsIndependentOfTheDisplayedLabel() {
        assertEquals("Automatic", InstallMode.Automatic.name)
        assertEquals("AlwaysAsk", InstallMode.AlwaysAsk.name)
    }

    @Test
    fun theReaderSurfacesEachStoredPreference() = runTest {
        val store = FakePreferenceStore()
        val reader = StoreBackedPreferenceReader(store)

        store.setIncludePrereleases(true)
        store.setInstallMode(InstallMode.AlwaysAsk)
        store.setAppearance(AppearanceMode.Dark)
        store.setDynamicColorEnabled(false)
        store.setInstallerPackage("com.android.vending")

        assertTrue(reader.includePrereleases().first())
        assertEquals(InstallMode.AlwaysAsk, reader.installMode().first())
        assertEquals(AppearanceMode.Dark, reader.appearance().first())
        assertFalse(reader.isDynamicColorEnabled().first())
        assertEquals("com.android.vending", reader.installerPackage().first())
    }

    @Test
    fun theReaderStartsFromTheDefaults() = runTest {
        val reader = StoreBackedPreferenceReader(FakePreferenceStore())

        assertFalse(reader.includePrereleases().first())
        assertEquals(InstallMode.Automatic, reader.installMode().first())
        assertEquals(AppearanceMode.System, reader.appearance().first())
        assertTrue(reader.isDynamicColorEnabled().first())
        assertEquals(SHELL_INSTALLER_PACKAGE, reader.installerPackage().first())
    }
}
