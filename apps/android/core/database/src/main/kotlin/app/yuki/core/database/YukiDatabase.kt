package app.yuki.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [InstallEntity::class, InstallProgressEntity::class, PackageIndexEntity::class],
    version = 5,
    exportSchema = true,
)
@TypeConverters(InstantConverter::class)
internal abstract class YukiDatabase : RoomDatabase() {
    abstract fun installDao(): InstallDao

    abstract fun installProgressDao(): InstallProgressDao

    abstract fun packageIndexDao(): PackageIndexDao
}

internal const val YUKI_DATABASE_NAME = "yuki.db"
