package app.yuki.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallProgressDao {
    @Query("SELECT * FROM install_progress WHERE githubRepoId = :githubRepoId")
    fun observeByRepoId(githubRepoId: Long): Flow<InstallProgressEntity?>

    @Query("SELECT * FROM install_progress ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<InstallProgressEntity>>

    @Query("SELECT * FROM install_progress WHERE githubRepoId = :githubRepoId")
    suspend fun findByRepoId(githubRepoId: Long): InstallProgressEntity?

    @Upsert
    suspend fun upsert(progress: InstallProgressEntity)

    @Query("DELETE FROM install_progress WHERE githubRepoId = :githubRepoId")
    suspend fun deleteByRepoId(githubRepoId: Long)

    @Query(
        """
        DELETE FROM install_progress
        WHERE status = 'installed'
          AND githubRepoId IN (SELECT githubRepoId FROM installs)
        """,
    )
    suspend fun deleteSettledInstalls()
}
