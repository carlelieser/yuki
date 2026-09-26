package app.yuki.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingUpdateDao {
    @Query("SELECT * FROM pending_updates")
    fun observeAll(): Flow<List<PendingUpdateEntity>>

    @Query("SELECT * FROM pending_updates")
    suspend fun getAll(): List<PendingUpdateEntity>

    @Query("SELECT * FROM pending_updates WHERE isNotified = 0")
    suspend fun getUnnotified(): List<PendingUpdateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<PendingUpdateEntity>)

    @Query("DELETE FROM pending_updates WHERE githubRepoId IN (:githubRepoIds)")
    suspend fun deleteByRepoIds(githubRepoIds: List<Long>)

    @Query("UPDATE pending_updates SET isNotified = 1 WHERE githubRepoId IN (:githubRepoIds)")
    suspend fun markNotified(githubRepoIds: List<Long>)

    @Transaction
    suspend fun replaceChecked(githubRepoIds: List<Long>, found: List<PendingUpdateEntity>) {
        val previous = getAll().associateBy(PendingUpdateEntity::githubRepoId)

        deleteByRepoIds(githubRepoIds)
        insertAll(found.map { entry -> entry.keepingNoticeFrom(previous[entry.githubRepoId]) })
    }
}
