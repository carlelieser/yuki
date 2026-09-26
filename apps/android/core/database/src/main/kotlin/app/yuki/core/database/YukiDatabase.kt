package app.yuki.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        InstallEntity::class,
        InstallProgressEntity::class,
        PackageIndexEntity::class,
        PendingUpdateEntity::class,
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 5, to = 6), AutoMigration(from = 6, to = 7)],
)
@TypeConverters(InstantConverter::class)
internal abstract class YukiDatabase : RoomDatabase() {
    abstract fun installDao(): InstallDao

    abstract fun installProgressDao(): InstallProgressDao

    abstract fun packageIndexDao(): PackageIndexDao

    abstract fun pendingUpdateDao(): PendingUpdateDao
}

internal const val YUKI_DATABASE_NAME = "yuki.db"
