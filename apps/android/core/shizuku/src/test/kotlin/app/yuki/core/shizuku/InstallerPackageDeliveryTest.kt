package app.yuki.core.shizuku

import app.yuki.core.installer.ApkIdentity
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallerPackageDeliveryTest {
    private val binder = RecordingUserServiceBinder()

    @Test
    fun `reads the chosen installer package for every install`() = runTest {
        val preference = RecordingInstallerPackagePreference("com.android.vending")

        runCatching { strategyWith(preference).install(MISSING_APK, IDENTITY).first() }
        runCatching { strategyWith(preference).install(MISSING_APK, IDENTITY).first() }

        assertEquals(2, preference.reads)
    }

    @Test
    fun `reads the installer package before it reaches the privileged installer`() = runTest {
        val preference = RecordingInstallerPackagePreference(SHELL_INSTALLER_PACKAGE)

        runCatching { strategyWith(preference).install(MISSING_APK, IDENTITY).first() }

        assertEquals(emptyList<String>(), binder.installer.installerPackages)
        assertEquals(1, preference.reads)
    }

    private fun strategyWith(
        preference: InstallerPackagePreference,
    ): ShizukuInstallStrategy = ShizukuInstallStrategy(binder, preference)
}

private val IDENTITY = ApkIdentity("com.acme.app", 7L)
private val MISSING_APK = File("absent.apk")

private class RecordingInstallerPackagePreference(private val stored: String) :
    InstallerPackagePreference {
    var reads: Int = 0
        private set

    override fun installerPackage(): Flow<String> {
        reads += 1

        return flowOf(stored)
    }
}
