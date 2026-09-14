package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StuckInstallReclaimerTest {
    private val progress = FakeInstallProgressStore()

    @Test
    fun `a download left behind by a dead process is marked failed`() = runTest {
        progress.write(progressOf(state = InstallState.Downloading(testSize(250L))))

        reclaimerOf().reclaim()

        assertEquals(TIMED_OUT, progress.find(42L)?.state)
    }

    @Test
    fun `a pending user action left behind by a dead process is marked failed`() = runTest {
        progress.write(progressOf(state = InstallState.PendingUserAction))

        reclaimerOf().reclaim()

        assertEquals(TIMED_OUT, progress.find(42L)?.state)
    }

    @Test
    fun `an install stranded mid-installation is reclaimed`() = runTest {
        progress.write(progressOf(state = InstallState.Installing))

        reclaimerOf().reclaim()

        assertEquals(TIMED_OUT, progress.find(42L)?.state)
    }

    @Test
    fun `an install whose work is still live is left alone`() = runTest {
        val downloading = InstallState.Downloading(testSize(250L))
        progress.write(progressOf(state = downloading))

        reclaimerOf(live = setOf(42L)).reclaim()

        assertEquals(downloading, progress.find(42L)?.state)
    }

    @Test
    fun `a row that already failed is not rewritten`() = runTest {
        val failed = InstallState.Failed(InstallFailure.InsufficientStorage)
        progress.write(progressOf(state = failed))

        reclaimerOf().reclaim()

        assertEquals(failed, progress.find(42L)?.state)
    }

    @Test
    fun `an installed row is never touched`() = runTest {
        val installed = InstallState.Installed("v1.2.0")
        progress.write(progressOf(state = installed))

        reclaimerOf().reclaim()

        assertEquals(installed, progress.find(42L)?.state)
    }

    @Test
    fun `reclaiming keeps the row identity so the library can still render it`() = runTest {
        progress.write(progressOf(state = InstallState.Downloading(testSize(250L))))

        reclaimerOf().reclaim()

        val reclaimed = progress.find(42L)
        assertEquals("Acme App", reclaimed?.target?.title)
        assertEquals("acme-app", reclaimed?.target?.slug)
        assertEquals("v1.2.0", reclaimed?.versionTag)
    }

    @Test
    fun `only the stranded rows are reclaimed when work is live for some`() = runTest {
        progress.write(progressOf(githubRepoId = 1L, state = InstallState.PendingUserAction))
        progress.write(progressOf(githubRepoId = 2L, state = InstallState.PendingUserAction))

        val reclaimed = reclaimerOf(live = setOf(2L)).reclaim()

        assertEquals(1, reclaimed.getOrNull())
        assertEquals(TIMED_OUT, progress.find(1L)?.state)
        assertEquals(InstallState.PendingUserAction, progress.find(2L)?.state)
    }

    @Test
    fun `a store failure is reported as a failed result rather than thrown`() = runTest {
        val reclaimer = StuckInstallReclaimer(ExplodingProgressStore, FakeInstallWorkLiveness())

        assertEquals(true, reclaimer.reclaim().isFailure)
    }

    private fun reclaimerOf(live: Set<Long> = emptySet()): StuckInstallReclaimer =
        StuckInstallReclaimer(progress, FakeInstallWorkLiveness(live))

    private fun progressOf(
        githubRepoId: Long = 42L,
        state: InstallState,
    ): InstallProgress = InstallProgress(
        target = InstallTarget(githubRepoId, "acme-app", "Acme App", iconUrl = null),
        versionTag = "v1.2.0",
        state = state,
    )
}

private val TIMED_OUT = InstallState.Failed(InstallFailure.TimedOut)

private class FakeInstallWorkLiveness(
    private val live: Set<Long> = emptySet(),
) : InstallWorkLiveness {
    override suspend fun isLive(githubRepoId: Long): Boolean = githubRepoId in live
}

private object ExplodingProgressStore : InstallProgressStore by FakeInstallProgressStore() {
    override suspend fun unsettled(): List<InstallProgress> =
        throw IllegalStateException("the progress store is unavailable")
}
