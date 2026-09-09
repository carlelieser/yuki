package app.yuki.core.installer

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test

class InstallStrategySelectorTest {
    private val fallback = FakeInstallStrategy(listOf(InstallOutcome.Succeeded))
    private val privileged = FakeInstallStrategy(listOf(InstallOutcome.Succeeded))

    @Test
    fun `falls back when no privileged installer is present`() = runTest {
        assertSame(fallback, selectorOf(privilegedInstaller = null).select())
    }

    @Test
    fun `falls back when the privileged installer is not ready`() = runTest {
        val selector = selectorOf(FakePrivilegedInstaller(isReady = false, strategy = privileged))

        assertSame(fallback, selector.select())
    }

    @Test
    fun `prefers the privileged installer when it is ready`() = runTest {
        val selector = selectorOf(FakePrivilegedInstaller(isReady = true, strategy = privileged))

        assertSame(privileged, selector.select())
    }

    private fun selectorOf(privilegedInstaller: PrivilegedInstaller?): InstallStrategySelector =
        InstallStrategySelector(
            identityReader = FakeApkIdentityReader(ApkIdentity("com.acme.app", 1L)),
            fallback = fallback,
            privileged = privilegedInstaller,
        )
}
