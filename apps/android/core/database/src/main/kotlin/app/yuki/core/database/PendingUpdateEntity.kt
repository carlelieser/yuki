package app.yuki.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_updates")
data class PendingUpdateEntity(
    @PrimaryKey val githubRepoId: Long,
    val versionTag: String,
    val isNotified: Boolean,
)

internal fun PendingUpdateEntity.keepingNoticeFrom(previous: PendingUpdateEntity?): PendingUpdateEntity {
    val isSameVersion = previous?.versionTag == versionTag
    val wasNotified = previous?.isNotified == true

    return if (isSameVersion && wasNotified) copy(isNotified = true) else this
}
