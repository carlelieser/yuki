package app.yuki.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface PackageIndexDao {
    @Query("SELECT * FROM package_index")
    suspend fun getAll(): List<PackageIndexEntity>

    @Query("SELECT COUNT(*) FROM package_index")
    suspend fun count(): Int

    @Query("DELETE FROM package_index")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<PackageIndexEntity>)

    @Transaction
    suspend fun replaceAll(entries: List<PackageIndexEntity>) {
        clear()
        insertAll(entries)
    }
}
