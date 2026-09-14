package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UninstallerTest {
    @Test
    fun removesThePackageWhenThePrivilegedInstallerIsReady() = runTest {
        val privileged = readyInstaller()

        val outcome = Uninstaller(privileged).uninstall(PACKAGE)

        assertEquals(UninstallOutcome.Removed, outcome)
        assertEquals(listOf(PACKAGE), privileged.uninstalled)
    }

    @Test
    fun asksForTheSystemPromptWhenThereIsNoPrivilegedInstaller() = runTest {
        val outcome = Uninstaller(null).uninstall(PACKAGE)

        assertEquals(UninstallOutcome.NeedsSystemPrompt, outcome)
    }

    @Test
    fun asksForTheSystemPromptWhenThePrivilegedInstallerIsNotReady() = runTest {
        val privileged = FakePrivilegedInstaller(
            isReady = false,
            strategy = FakeInstallStrategy(emptyList()),
        )

        val outcome = Uninstaller(privileged).uninstall(PACKAGE)

        assertEquals(UninstallOutcome.NeedsSystemPrompt, outcome)
        assertTrue(privileged.uninstalled.isEmpty())
    }

    @Test
    fun reportsTheReasonWhenAPrivilegedUninstallFails() = runTest {
        val privileged = FakePrivilegedInstaller(
            isReady = true,
            strategy = FakeInstallStrategy(emptyList()),
            outcome = InstallOutcome.Failed(InstallFailure.Aborted),
        )

        val outcome = Uninstaller(privileged).uninstall(PACKAGE)

        assertEquals(UninstallOutcome.Failed(InstallFailure.Aborted), outcome)
    }

    @Test
    fun isSilentOnlyWhenThePrivilegedInstallerIsReady() = runTest {
        assertTrue(Uninstaller(readyInstaller()).isSilent())
        assertFalse(Uninstaller(null).isSilent())
    }

    private fun readyInstaller() =
        FakePrivilegedInstaller(isReady = true, strategy = FakeInstallStrategy(emptyList()))
}

private const val PACKAGE = "com.acme.app"
