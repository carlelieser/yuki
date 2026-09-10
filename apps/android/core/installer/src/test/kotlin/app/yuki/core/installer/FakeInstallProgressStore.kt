package app.yuki.core.installer

import app.yuki.core.model.InstallState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeInstallProgressStore : InstallProgressStore {
    private val rows = MutableStateFlow<Map<Long, InstallProgress>>(emptyMap())

    val written: MutableList<InstallProgress> = mutableListOf()

    override fun observe(githubRepoId: Long): Flow<InstallProgress?> =
        rows.map { current -> current[githubRepoId] }

    override fun observeActive(): Flow<List<InstallProgress>> =
        rows.map { current -> current.values.toList() }

    override suspend fun find(githubRepoId: Long): InstallProgress? = rows.value[githubRepoId]

    override suspend fun write(progress: InstallProgress) {
        written += progress
        rows.value = rows.value + (progress.githubRepoId to progress)
    }

    override suspend fun clear(githubRepoId: Long) {
        rows.value = rows.value - githubRepoId
    }

    override suspend fun clearSettled() {
        rows.value = rows.value.filterValues { progress ->
            progress.state !is InstallState.Installed
        }
    }
}
