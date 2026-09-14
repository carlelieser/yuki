package app.yuki.core.database

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallProgressDao {
    @Query("SELECT * FROM install_progress WHERE githubRepoId = :githubRepoId")
    fun observeByRepoId(githubRepoId: Long): Flow<InstallProgressEntity?>

    @Query("SELECT * FROM install_progress ORDER BY createdAt ASC, githubRepoId ASC")
    fun observeAll(): Flow<List<InstallProgressEntity>>

    @Query("SELECT * FROM install_progress WHERE githubRepoId = :githubRepoId")
    suspend fun findByRepoId(githubRepoId: Long): InstallProgressEntity?

    @Query(
        """
        SELECT * FROM install_progress
        WHERE status IN ('queued', 'downloading', 'installing', 'pending_user_action')
        """,
    )
    suspend fun findUnsettled(): List<InstallProgressEntity>

    @Query(
        """
        INSERT INTO install_progress (
            githubRepoId, slug, title, iconUrl, status, versionTag,
            bytesDownloaded, bytesTotal, failureReason, failureMessage,
            createdAt, updatedAt
        )
        VALUES (
            :githubRepoId, :slug, :title, :iconUrl, :status, :versionTag,
            :bytesDownloaded, :bytesTotal, :failureReason, :failureMessage,
            :now, :now
        )
        ON CONFLICT(githubRepoId) DO UPDATE SET
            slug = excluded.slug,
            title = excluded.title,
            iconUrl = excluded.iconUrl,
            status = excluded.status,
            versionTag = excluded.versionTag,
            bytesDownloaded = excluded.bytesDownloaded,
            bytesTotal = excluded.bytesTotal,
            failureReason = excluded.failureReason,
            failureMessage = excluded.failureMessage,
            updatedAt = excluded.updatedAt
        """,
    )
    suspend fun upsert(
        githubRepoId: Long,
        slug: String,
        title: String,
        iconUrl: String?,
        status: String,
        versionTag: String,
        bytesDownloaded: Long,
        bytesTotal: Long,
        failureReason: String?,
        failureMessage: String?,
        now: Long,
    )

    @Query("DELETE FROM install_progress WHERE githubRepoId = :githubRepoId")
    suspend fun deleteByRepoId(githubRepoId: Long)

    @Query("DELETE FROM install_progress WHERE status = 'installed'")
    suspend fun deleteSettledInstalls()
}
