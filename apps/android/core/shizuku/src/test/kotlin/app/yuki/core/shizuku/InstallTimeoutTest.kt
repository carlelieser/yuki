package app.yuki.core.shizuku

import app.yuki.core.installer.InstallException
import app.yuki.core.model.InstallFailure
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallTimeoutTest {
    @Test
    fun `a privileged call that never returns fails instead of hanging`() = runTest {
        val failure = timeoutOf { CompletableDeferred<Unit>().await() }

        assertEquals(InstallFailure.TimedOut, failure.failure)
    }

    @Test
    fun `a timeout surfaces as a TimedOut install failure not a cancellation`() = runTest {
        val error = runCatching {
            withInstallTimeout(TIMEOUT, MESSAGE) { CompletableDeferred<Unit>().await() }
        }.exceptionOrNull()

        assertTrue("A timeout must not escape as $error", error is InstallException)
        assertTrue("A timeout must not escape as a cancellation", error !is CancellationException)
    }

    @Test
    fun `a timeout explains which privileged call gave up`() = runTest {
        val failure = timeoutOf { CompletableDeferred<Unit>().await() }

        assertEquals(MESSAGE, failure.message)
    }

    @Test
    fun `a privileged call that returns in time keeps its result`() = runTest {
        assertEquals("installed", withInstallTimeout(TIMEOUT, MESSAGE) { "installed" })
    }

    @Test
    fun `a genuine cancellation of the caller is not swallowed as a timeout`() = runTest {
        val started = CompletableDeferred<Unit>()
        val call = async {
            withInstallTimeout(TIMEOUT, MESSAGE) {
                started.complete(Unit)
                CompletableDeferred<Unit>().await()
            }
        }

        started.await()
        call.cancel()

        assertTrue(runCatching { call.await() }.exceptionOrNull() is CancellationException)
    }
}

private suspend fun timeoutOf(block: suspend () -> Unit): InstallException {
    val error = runCatching { withInstallTimeout(TIMEOUT, MESSAGE, block) }.exceptionOrNull()

    return error as? InstallException
        ?: throw AssertionError("Expected an InstallException but got $error")
}

private val TIMEOUT = 1.seconds
private const val MESSAGE = "the privileged installer did not respond"
