package app.yuki.core.installer

import app.yuki.core.model.InstallState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class InstallScheduler @Inject internal constructor(
    private val queue: InstallWorkQueue,
    private val progress: InstallProgressStore,
) {
    fun observe(githubRepoId: Long): Flow<InstallProgress?> = progress.observe(githubRepoId)

    fun observeActive(): Flow<List<InstallProgress>> = progress.observeActive()

    suspend fun start(request: InstallRequest) {
        try {
            queue.enqueue(request)
        } catch (error: InstallException) {
            val failed = InstallState.Failed(error.failure)
            progress.write(InstallProgress(request.target, request.source.versionTag, failed))
        }
    }

    suspend fun cancel(githubRepoId: Long) {
        queue.cancel(githubRepoId)
        progress.clear(githubRepoId)
    }

    suspend fun forget(githubRepoId: Long) = progress.clear(githubRepoId)
}
