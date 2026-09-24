package app.yuki.core.installer

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallModeGatedInstallerTest {
    private val strategy = FakeInstallStrategy(listOf(InstallOutcome.Succeeded))
    private val ready = FakePrivilegedInstaller(isReady = true, strategy = strategy)

    @Test
    fun `stays ready while the privileged installer is preferred`() = runTest {
        assertTrue(gated(ready, PreferredInstaller.Privileged).isReady())
    }

    @Test
    fun `reports not ready when the system installer is preferred`() = runTest {
        assertFalse(gated(ready, PreferredInstaller.System).isReady())
    }

    @Test
    fun `does not become ready just because it is preferred`() = runTest {
        val notReady = FakePrivilegedInstaller(isReady = false, strategy = strategy)

        assertFalse(gated(notReady, PreferredInstaller.Privileged).isReady())
    }

    @Test
    fun `hands out the wrapped strategy and uninstall`() = runTest {
        val installer = gated(ready, PreferredInstaller.Privileged)

        assertSame(strategy, installer.strategy())
        installer.uninstall("com.acme.app")
        assertEquals(listOf("com.acme.app"), ready.uninstalled)
    }

    private fun gated(inner: PrivilegedInstaller, preferred: PreferredInstaller): PrivilegedInstaller =
        InstallModeGatedInstaller(inner) { flowOf(preferred) }
}
