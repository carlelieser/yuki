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
    fun theInstallSourceAppliesOnlyToPrivilegedInstalls() {
        assertTrue(installSourceApplies(InstallMode.Shizuku))
        assertFalse(installSourceApplies(InstallMode.System))
    }

    @Test
    fun aPresetPackageResolvesToItsPresetLabel() {
        assertEquals(
            InstallSourceName.Preset(R.string.settings_install_source_shell),
            installSourceName(SHELL_INSTALLER_PACKAGE, null),
        )
        assertEquals(
            InstallSourceName.Preset(R.string.settings_install_source_play_store),
            installSourceName(PLAY_STORE_PACKAGE, "Play Store"),
        )
    }

    @Test
    fun anInstalledAppResolvesToItsOwnLabel() {
        assertEquals(
            InstallSourceName.App("Acme Store"),
            installSourceName("com.acme.store", "Acme Store"),
        )
    }

    @Test
    fun anAbsentAppResolvesToItsPackageName() {
        assertEquals(
            InstallSourceName.Package("com.gone.app"),
            installSourceName("com.gone.app", null),
        )
    }
}

private fun labelsOf(apps: List<InstalledApp>): List<String> = apps.map(InstalledApp::label)
