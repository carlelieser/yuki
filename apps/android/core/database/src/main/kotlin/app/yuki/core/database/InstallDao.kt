package app.yuki.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallDao {
    @Query("SELECT * FROM installs ORDER BY installedAt DESC")
    fun observeAll(): Flow<List<InstallEntity>>

    @Query("SELECT * FROM installs ORDER BY installedAt DESC")
    suspend fun getAll(): List<InstallEntity>

    @Query("SELECT * FROM installs WHERE githubRepoId = :githubRepoId")
    suspend fun findByRepoId(githubRepoId: Long): InstallEntity?

    @Upsert
    suspend fun upsert(install: InstallEntity)

    @Query("DELETE FROM installs WHERE githubRepoId = :githubRepoId")
    suspend fun deleteByRepoId(githubRepoId: Long)

    @Query("DELETE FROM installs WHERE packageName IN (:packageNames)")
    suspend fun deleteByPackageNames(packageNames: List<String>)
}
