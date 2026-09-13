package app.yuki.core.installer

import app.yuki.core.model.InstallState
import kotlinx.coroutines.flow.Flow

data class InstallProgress(
    val target: InstallTarget,
    val versionTag: String,
    val state: InstallState,
) {
    val githubRepoId: Long get() = target.githubRepoId
}

interface InstallProgressStore {
    fun observe(githubRepoId: Long): Flow<InstallProgress?>

    fun observeActive(): Flow<List<InstallProgress>>

    suspend fun find(githubRepoId: Long): InstallProgress?

    suspend fun write(progress: InstallProgress)

    suspend fun clear(githubRepoId: Long)

    suspend fun clearSettled()

    suspend fun clearFailed()
}
