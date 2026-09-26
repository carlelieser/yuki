package app.yuki.core.database

import app.yuki.core.model.AvailableUpdate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PendingUpdate(
    val githubRepoId: Long,
    val versionTag: String,
    val isNotified: Boolean,
)

data class CheckedUpdates(
    val githubRepoIds: Set<Long>,
    val updates: List<AvailableUpdate>,
)

interface PendingUpdateStore {
    fun observe(): Flow<List<PendingUpdate>>

    suspend fun replace(checked: CheckedUpdates, isSeen: Boolean)

    suspend fun unnotified(): List<PendingUpdate>

    suspend fun markNotified(githubRepoIds: List<Long>)
}

@Singleton
internal class RoomPendingUpdateStore @Inject constructor(
    private val dao: PendingUpdateDao,
) : PendingUpdateStore {
    override fun observe(): Flow<List<PendingUpdate>> =
        dao.observeAll().map { entities -> entities.map(PendingUpdateEntity::toPendingUpdate) }

    override suspend fun replace(checked: CheckedUpdates, isSeen: Boolean) {
        val found = checked.updates.map { update -> update.toEntity(isNotified = isSeen) }

        dao.replaceChecked(checked.githubRepoIds.toList(), found)
    }

    override suspend fun unnotified(): List<PendingUpdate> =
        dao.getUnnotified().map(PendingUpdateEntity::toPendingUpdate)

    override suspend fun markNotified(githubRepoIds: List<Long>) {
        if (githubRepoIds.isEmpty()) return

        dao.markNotified(githubRepoIds)
    }
}

private fun PendingUpdateEntity.toPendingUpdate(): PendingUpdate = PendingUpdate(
    githubRepoId = githubRepoId,
    versionTag = versionTag,
    isNotified = isNotified,
)

private fun AvailableUpdate.toEntity(isNotified: Boolean): PendingUpdateEntity = PendingUpdateEntity(
    githubRepoId = installed.githubRepoId,
    versionTag = version.tag,
    isNotified = isNotified,
)
