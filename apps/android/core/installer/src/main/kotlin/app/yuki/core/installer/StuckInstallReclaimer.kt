package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StuckInstallReclaimer @Inject internal constructor(
    private val progress: InstallProgressStore,
    private val liveness: InstallWorkLiveness,
) {
    suspend fun reclaim(): Result<Int> = runCatching {
        val stranded = progress.unsettled().filterNot { row -> liveness.isLive(row.githubRepoId) }

        stranded.forEach { row -> progress.write(row.copy(state = TIMED_OUT)) }

        stranded.size
    }
}

private val TIMED_OUT = InstallState.Failed(InstallFailure.TimedOut)
