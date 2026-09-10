package app.yuki.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "install_progress")
data class InstallProgressEntity(
    @PrimaryKey val githubRepoId: Long,
    val status: String,
    val versionTag: String,
    val bytesDownloaded: Long,
    val bytesTotal: Long,
    val failureReason: String?,
    val failureMessage: String?,
    val updatedAt: Long,
)
