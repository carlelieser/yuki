package app.yuki.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_1_TO_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `install_progress` (
                `githubRepoId` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `versionTag` TEXT NOT NULL,
                `bytesDownloaded` INTEGER NOT NULL,
                `bytesTotal` INTEGER NOT NULL,
                `failureReason` TEXT,
                `failureMessage` TEXT,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`githubRepoId`)
            )
            """.trimIndent(),
        )
    }
}

internal val YUKI_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_TO_2)
