package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstallSchedulerTest {
    @Test
    fun `an install that could not be queued is stored as failed`() = runTest {
        val progress = FakeInstallProgressStore()
        val scheduler = InstallScheduler(RefusingWorkQueue(), progress)

        scheduler.start(testRequest(githubRepoId = 5L))

        assertEquals(
            InstallState.Failed(InstallFailure.ScheduleFailed),
            progress.find(5L)?.state,
        )
    }

    @Test
    fun `a queued install writes nothing until its worker runs`() = runTest {
        val progress = FakeInstallProgressStore()
        val queue = RecordingWorkQueue()

        InstallScheduler(queue, progress).start(testRequest(githubRepoId = 5L))

        assertEquals(listOf(5L), queue.enqueued)
        assertNull(progress.find(5L))
    }
}

private class RefusingWorkQueue : InstallWorkQueue {
    override suspend fun enqueue(request: InstallRequest) =
        throw InstallException(InstallFailure.ScheduleFailed, "disk full")

    override fun cancel(githubRepoId: Long) = Unit
}

private class RecordingWorkQueue : InstallWorkQueue {
    val enqueued: MutableList<Long> = mutableListOf()

    override suspend fun enqueue(request: InstallRequest) {
        enqueued += request.target.githubRepoId
    }

    override fun cancel(githubRepoId: Long) = Unit
}
