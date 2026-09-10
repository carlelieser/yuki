package app.yuki.core.database

import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallProgressStore
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class RoomInstallProgressStore @Inject constructor(
    private val dao: InstallProgressDao,
    private val clock: Clock,
) : InstallProgressStore {
    override fun observe(githubRepoId: Long): Flow<InstallProgress?> =
        dao.observeByRepoId(githubRepoId).map { entity -> entity?.toProgress() }

    override fun observeActive(): Flow<List<InstallProgress>> =
        dao.observeAll().map { entities -> entities.map(InstallProgressEntity::toProgress) }

    override suspend fun find(githubRepoId: Long): InstallProgress? =
        dao.findByRepoId(githubRepoId)?.toProgress()

    override suspend fun write(progress: InstallProgress) =
        dao.upsert(progress.toEntity(clock.millis()))

    override suspend fun clear(githubRepoId: Long) = dao.deleteByRepoId(githubRepoId)
}
