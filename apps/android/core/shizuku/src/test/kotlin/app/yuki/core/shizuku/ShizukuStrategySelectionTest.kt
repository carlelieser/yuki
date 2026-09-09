package app.yuki.core.shizuku

import app.yuki.core.installer.ApkIdentity
import app.yuki.core.installer.ApkIdentityReader
import app.yuki.core.installer.InstallOutcome
import app.yuki.core.installer.InstallStrategy
import app.yuki.core.installer.InstallStrategySelector
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Test

class ShizukuStrategySelectionTest {
    private val gateway = FakeShizukuGateway()
    private val monitor = ShizukuMonitor(gateway)
    private val binder = FakeUserServiceBinder()
    private val silent = ShizukuInstallStrategy(binder)
    private val privileged = ShizukuPrivilegedInstaller(monitor, silent)
    private val fallback = RecordingStrategy()

    @Test
    fun `selects the silent strategy when shizuku is ready`() = runTest {
        monitor.start()

        assertSame(silent, selector().select())
    }

    @Test
    fun `falls back when shizuku is not installed`() = runTest {
        gateway.installed = false
        monitor.start()

        assertSame(fallback, selector().select())
    }

    @Test
    fun `falls back when shizuku is installed but not running`() = runTest {
        gateway.running = false
        monitor.start()

        assertSame(fallback, selector().select())
    }

    @Test
    fun `falls back when permission has not been granted`() = runTest {
        gateway.permission = DENIED
        monitor.start()

        assertSame(fallback, selector().select())
    }

    @Test
    fun `re-evaluates readiness rather than trusting the last known state`() = runTest {
        monitor.start()
        assertSame(silent, selector().select())

        gateway.running = false

        assertSame(fallback, selector().select())
    }

    private fun selector(): InstallStrategySelector = InstallStrategySelector(
        identityReader = FixedIdentityReader,
        fallback = fallback,
        privileged = privileged,
    )
}

private val IDENTITY = ApkIdentity("com.acme.app", 7L)

private object FixedIdentityReader : ApkIdentityReader {
    override fun read(apk: File): ApkIdentity = IDENTITY
}

private class RecordingStrategy : InstallStrategy {
    override fun install(apk: File, identity: ApkIdentity): Flow<InstallOutcome> =
        flowOf(InstallOutcome.Succeeded)
}
