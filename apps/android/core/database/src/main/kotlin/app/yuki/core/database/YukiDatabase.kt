package app.yuki.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [InstallEntity::class], version = 1, exportSchema = false)
@TypeConverters(InstantConverter::class)
internal abstract class YukiDatabase : RoomDatabase() {
    abstract fun installDao(): InstallDao
}

internal const val YUKI_DATABASE_NAME = "yuki.db"
