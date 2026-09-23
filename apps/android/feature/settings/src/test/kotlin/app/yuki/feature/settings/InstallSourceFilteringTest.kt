package app.yuki.feature.settings

import app.yuki.core.shizuku.SHELL_INSTALLER_PACKAGE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallSourceFilteringTest {
    private val apps = listOf(
        installedAppOf("com.acme.store", "Acme Store"),
        installedAppOf(PLAY_STORE_PACKAGE, "Google Play Store"),
        installedAppOf("net.example.files", "Files"),
    )

    @Test
    fun anEmptyQueryKeepsEveryApp() {
        assertEquals(apps, matchingApps(apps, ""))
    }

    @Test
    fun aBlankQueryKeepsEveryApp() {
        assertEquals(apps, matchingApps(apps, "   "))
    }

    @Test
    fun aQueryMatchesTheLabelWithoutRegardForCase() {
        assertEquals(listOf("Acme Store"), labelsOf(matchingApps(apps, "acme")))
    }

    @Test
    fun aQueryMatchesThePackageName() {
        assertEquals(listOf("Files"), labelsOf(matchingApps(apps, "net.example")))
    }

    @Test
    fun aQueryThatMatchesNothingKeepsNoApp() {
        assertEquals(emptyList<String>(), labelsOf(matchingApps(apps, "zzz")))
    }

    @Test
    fun theInstallSourceAppliesOnlyToSilentInstalls() {
        assertTrue(installSourceApplies(InstallMode.Automatic))
        assertFalse(installSourceApplies(InstallMode.AlwaysAsk))
    }

    @Test
    fun aPresetPackageResolvesToItsPresetLabel() {
        assertEquals("Shell", installSourceLabel(SHELL_INSTALLER_PACKAGE, null))
        assertEquals("Google Play Store", installSourceLabel(PLAY_STORE_PACKAGE, "Play Store"))
    }

    @Test
    fun anInstalledAppResolvesToItsOwnLabel() {
        assertEquals("Acme Store", installSourceLabel("com.acme.store", "Acme Store"))
    }

    @Test
    fun anAbsentAppResolvesToItsPackageName() {
        assertEquals("com.gone.app", installSourceLabel("com.gone.app", null))
    }
}

private fun labelsOf(apps: List<InstalledApp>): List<String> = apps.map(InstalledApp::label)
