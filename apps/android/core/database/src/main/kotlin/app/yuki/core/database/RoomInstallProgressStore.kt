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

    override suspend fun unsettled(): List<InstallProgress> =
        dao.findUnsettled().map(InstallProgressEntity::toProgress)

    override suspend fun write(progress: InstallProgress) {
        val now = clock.millis()
        val entity = progress.toEntity(now)

        dao.upsert(
            githubRepoId = entity.githubRepoId,
            slug = entity.slug,
            title = entity.title,
            iconUrl = entity.iconUrl,
            status = entity.status,
            versionTag = entity.versionTag,
            bytesDownloaded = entity.bytesDownloaded,
            bytesTotal = entity.bytesTotal,
            failureReason = entity.failureReason,
            failureMessage = entity.failureMessage,
            now = now,
        )
    }

    override suspend fun clear(githubRepoId: Long) = dao.deleteByRepoId(githubRepoId)

    override suspend fun clearSettled() = dao.deleteSettledInstalls()
}
