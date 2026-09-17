package app.yuki.install

import app.yuki.feature.library.DetectedInstallRefresh
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRefreshObserverTest {
    @Test
    fun `reconciles when the app comes to the foreground`() = runTest {
        val detection = CountingDetection()
        val observer = observerFor(detection, this)

        observer.refresh()
        advanceUntilIdle()

        assertEquals(1, detection.calls)
    }

    @Test
    fun `reconciles again on a later foreground`() = runTest {
        val detection = CountingDetection()
        val observer = observerFor(detection, this)

        observer.refresh()
        advanceUntilIdle()
        observer.refresh()
        advanceUntilIdle()

        assertEquals(2, detection.calls)
    }

    @Test
    fun `ignores a foreground event while a reconcile is still running`() = runTest {
        val detection = BlockingDetection()
        val observer = observerFor(detection, this)

        observer.refresh()
        runCurrent()
        assertEquals(1, detection.started)

        observer.refresh()
        runCurrent()
        assertEquals(1, detection.started)

        detection.release()
        advanceUntilIdle()

        observer.refresh()
        advanceUntilIdle()
        assertEquals(2, detection.started)
    }

    @Test
    fun `keeps reconciling after a failed run`() = runTest {
        val detection = CountingDetection(failing = true)
        val observer = observerFor(detection, this)

        observer.refresh()
        advanceUntilIdle()
        observer.refresh()
        advanceUntilIdle()

        assertEquals(2, detection.calls)
    }

    private fun observerFor(
        detection: DetectedInstallRefresh,
        scope: TestScope,
    ): LibraryRefreshObserver = LibraryRefreshObserver(detection, scope) { _, _ -> }
}

private class CountingDetection(private val failing: Boolean = false) : DetectedInstallRefresh {
    var calls = 0
        private set

    override suspend fun reconcile() {
        calls += 1
        if (failing) throw IllegalStateException("detection failed")
    }
}

private class BlockingDetection : DetectedInstallRefresh {
    var started = 0
        private set

    private val gate = CompletableDeferred<Unit>()

    fun release() = gate.complete(Unit)

    override suspend fun reconcile() {
        started += 1
        gate.await()
    }
}
