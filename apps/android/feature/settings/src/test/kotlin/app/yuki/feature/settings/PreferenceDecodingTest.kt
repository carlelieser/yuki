package app.yuki.feature.settings

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
        assertTrue(YukiPreferences.Defaults.isDynamicColorEnabled)
    }

    @Test
    fun theInstallModeSwitchMapsBothWays() {
        assertEquals(InstallMode.Automatic, installModeFor(isAutomatic = true))
        assertEquals(InstallMode.AlwaysAsk, installModeFor(isAutomatic = false))
        assertEquals(InstallMode.AlwaysAsk, nextMode(InstallMode.Automatic))
        assertEquals(InstallMode.Automatic, nextMode(InstallMode.AlwaysAsk))
    }

    @Test
    fun eachInstallModeExplainsItself() {
        assertEquals(INSTALL_MODE_AUTOMATIC, installModeSupporting(InstallMode.Automatic))
        assertEquals(INSTALL_MODE_ALWAYS_ASK, installModeSupporting(InstallMode.AlwaysAsk))
    }

    @Test
    fun theReaderSurfacesEachStoredPreference() = runTest {
        val store = FakePreferenceStore()
        val reader = StoreBackedPreferenceReader(store)

        store.setIncludePrereleases(true)
        store.setInstallMode(InstallMode.AlwaysAsk)
        store.setDynamicColorEnabled(false)

        assertTrue(reader.includePrereleases().first())
        assertEquals(InstallMode.AlwaysAsk, reader.installMode().first())
        assertFalse(reader.isDynamicColorEnabled().first())
    }

    @Test
    fun theReaderStartsFromTheDefaults() = runTest {
        val reader = StoreBackedPreferenceReader(FakePreferenceStore())

        assertFalse(reader.includePrereleases().first())
        assertEquals(InstallMode.Automatic, reader.installMode().first())
        assertTrue(reader.isDynamicColorEnabled().first())
    }
}
