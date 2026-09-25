package app.yuki.core.shizuku

import app.yuki.core.installer.InstallException
import app.yuki.core.model.InstallFailure
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceConnectionAdapterTest {
    @Test
    fun `a disconnected installer service fails the install session`() = runTest {
        val error = runCatching {
            suspendCancellableCoroutine<IYukiInstaller> { continuation ->
                ServiceConnectionAdapter(continuation) {}.onServiceDisconnected(null)
            }
        }.exceptionOrNull()

        assertEquals(InstallFailure.SessionFailed, (error as InstallException).failure)
    }

    @Test
    fun `a missing installer binder fails the install session`() = runTest {
        val error = runCatching {
            suspendCancellableCoroutine<IYukiInstaller> { continuation ->
                ServiceConnectionAdapter(continuation) {}.onServiceConnected(null, null)
            }
        }.exceptionOrNull()

        assertEquals(InstallFailure.SessionFailed, (error as InstallException).failure)
    }
}
