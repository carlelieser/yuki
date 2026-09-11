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
