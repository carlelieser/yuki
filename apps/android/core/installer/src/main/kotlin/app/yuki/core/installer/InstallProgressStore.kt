package app.yuki.core.installer

import app.yuki.core.model.InstallState
import kotlinx.coroutines.flow.Flow

data class InstallProgress(
    val githubRepoId: Long,
    val versionTag: String,
    val state: InstallState,
)

interface InstallProgressStore {
    fun observe(githubRepoId: Long): Flow<InstallProgress?>

    fun observeActive(): Flow<List<InstallProgress>>

    suspend fun find(githubRepoId: Long): InstallProgress?

    suspend fun write(progress: InstallProgress)

    suspend fun clear(githubRepoId: Long)
}
