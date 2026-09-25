package app.yuki.core.installer

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

    fun start(request: InstallRequest) = queue.enqueue(request)

    suspend fun cancel(githubRepoId: Long) {
        queue.cancel(githubRepoId)
        progress.clear(githubRepoId)
    }

    suspend fun forget(githubRepoId: Long) = progress.clear(githubRepoId)
}
