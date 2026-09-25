package app.yuki.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "install_progress")
data class InstallProgressEntity(
    @PrimaryKey val githubRepoId: Long,
    val slug: String,
    val title: String,
    val iconUrl: String?,
    val status: String,
    val versionTag: String,
    val bytesDownloaded: Long,
    val bytesTotal: Long,
    val failureReason: String?,
    val failureMessage: String?,
    val failureCode: Int?,
    @ColumnInfo(defaultValue = "0") val createdAt: Long,
    val updatedAt: Long,
)
