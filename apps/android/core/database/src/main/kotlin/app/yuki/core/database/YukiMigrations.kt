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

internal val MIGRATION_2_TO_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `install_progress` ADD COLUMN `slug` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `install_progress` ADD COLUMN `title` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `install_progress` ADD COLUMN `iconUrl` TEXT")
        db.execSQL(
            """
            UPDATE `install_progress` SET
                `slug` = COALESCE(
                    (SELECT `slug` FROM `installs`
                     WHERE `installs`.`githubRepoId` = `install_progress`.`githubRepoId`),
                    ''
                ),
                `title` = COALESCE(
                    (SELECT `title` FROM `installs`
                     WHERE `installs`.`githubRepoId` = `install_progress`.`githubRepoId`),
                    ''
                ),
                `iconUrl` = (SELECT `iconUrl` FROM `installs`
                    WHERE `installs`.`githubRepoId` = `install_progress`.`githubRepoId`)
            """.trimIndent(),
        )
        db.execSQL("DELETE FROM `install_progress` WHERE `title` = ''")
    }
}

internal val MIGRATION_3_TO_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `install_progress` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL("UPDATE `install_progress` SET `createdAt` = `updatedAt`")
    }
}

internal val YUKI_MIGRATIONS: Array<Migration> =
    arrayOf(MIGRATION_1_TO_2, MIGRATION_2_TO_3, MIGRATION_3_TO_4)
