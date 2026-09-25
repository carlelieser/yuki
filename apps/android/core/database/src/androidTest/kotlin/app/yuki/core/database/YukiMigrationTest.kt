package app.yuki.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DATABASE = "yuki-migration-test.db"

@RunWith(AndroidJUnit4::class)
class YukiMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        YukiDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migratingFromVersionOneKeepsRecordedInstalls() {
        helper.createDatabase(TEST_DATABASE, 1).use { database ->
            database.execSQL(
                """
                INSERT INTO installs
                (githubRepoId, packageName, slug, title, iconUrl, versionTag, versionCode, installedAt)
                VALUES (7, 'com.termux', 'termux', 'Termux', NULL, 'v0.118.0', 118, 1000)
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, MIGRATION_1_TO_2)

        migrated.query("SELECT packageName, versionTag FROM installs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("com.termux", cursor.getString(0))
            assertEquals("v0.118.0", cursor.getString(1))
        }
    }

    @Test
    fun migratingFromVersionOneAddsAnEmptyInstallProgressTable() {
        helper.createDatabase(TEST_DATABASE, 1).close()

        val migrated = helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, MIGRATION_1_TO_2)

        migrated.query("SELECT COUNT(*) FROM install_progress").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun theMigratedInstallProgressTableAcceptsAProgressRow() {
        helper.createDatabase(TEST_DATABASE, 1).close()

        val migrated = helper.runMigrationsAndValidate(TEST_DATABASE, 2, true, MIGRATION_1_TO_2)
        migrated.execSQL(
            """
            INSERT INTO install_progress
            (githubRepoId, status, versionTag, bytesDownloaded, bytesTotal,
             failureReason, failureMessage, updatedAt)
            VALUES (7, 'downloading', 'v0.119.0', 4100000, 12100000, NULL, NULL, 2000)
            """.trimIndent(),
        )

        migrated.query("SELECT bytesDownloaded, bytesTotal FROM install_progress").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(4_100_000L, cursor.getLong(0))
            assertEquals(12_100_000L, cursor.getLong(1))
        }
    }

    @Test
    fun migratingToVersionThreeKeepsRecordedInstalls() {
        seedVersionTwo(
            """
            INSERT INTO installs
            (githubRepoId, packageName, slug, title, iconUrl, versionTag, versionCode, installedAt)
            VALUES (7, 'com.termux', 'termux', 'Termux', 'https://icons.test/t.png', 'v1', 1, 1000)
            """.trimIndent(),
        )

        val migrated = runToVersionThree()

        migrated.query("SELECT packageName, versionTag FROM installs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("com.termux", cursor.getString(0))
            assertEquals("v1", cursor.getString(1))
        }
    }

    @Test
    fun migratingToVersionThreeBackfillsProgressIdentityFromTheInstalledApp() {
        seedVersionTwo(
            """
            INSERT INTO installs
            (githubRepoId, packageName, slug, title, iconUrl, versionTag, versionCode, installedAt)
            VALUES (7, 'com.termux', 'termux', 'Termux', 'https://icons.test/t.png', 'v1', 1, 1000)
            """.trimIndent(),
            """
            INSERT INTO install_progress
            (githubRepoId, status, versionTag, bytesDownloaded, bytesTotal,
             failureReason, failureMessage, updatedAt)
            VALUES (7, 'downloading', 'v2', 4100000, 12100000, NULL, NULL, 2000)
            """.trimIndent(),
        )

        val migrated = runToVersionThree()

        migrated.query(
            "SELECT slug, title, iconUrl, bytesDownloaded FROM install_progress",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("termux", cursor.getString(0))
            assertEquals("Termux", cursor.getString(1))
            assertEquals("https://icons.test/t.png", cursor.getString(2))
            assertEquals(4_100_000L, cursor.getLong(3))
        }
    }

    @Test
    fun migratingToVersionThreeDropsProgressThatCannotBeIdentified() {
        seedVersionTwo(
            """
            INSERT INTO install_progress
            (githubRepoId, status, versionTag, bytesDownloaded, bytesTotal,
             failureReason, failureMessage, updatedAt)
            VALUES (99, 'installed', 'v2', 10, 10, NULL, NULL, 2000)
            """.trimIndent(),
        )

        val migrated = runToVersionThree()

        migrated.query("SELECT COUNT(*) FROM install_progress").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun theVersionThreeProgressTableAcceptsARowForAnAppThatIsNotInstalled() {
        seedVersionTwo()

        val migrated = runToVersionThree()
        migrated.execSQL(
            """
            INSERT INTO install_progress
            (githubRepoId, slug, title, iconUrl, status, versionTag, bytesDownloaded,
             bytesTotal, failureReason, failureMessage, updatedAt)
            VALUES (5, 'aurora', 'Aurora', NULL, 'downloading', 'v4', 500, 1000, NULL, NULL, 3000)
            """.trimIndent(),
        )

        migrated.query("SELECT title, bytesTotal FROM install_progress").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Aurora", cursor.getString(0))
            assertEquals(1_000L, cursor.getLong(1))
        }
    }

    @Test
    fun migratingToVersionFourBackfillsCreatedAtFromUpdatedAt() {
        seedVersionThree(
            """
            INSERT INTO install_progress
            (githubRepoId, slug, title, iconUrl, status, versionTag, bytesDownloaded,
             bytesTotal, failureReason, failureMessage, updatedAt)
            VALUES (5, 'aurora', 'Aurora', NULL, 'downloading', 'v4', 500, 1000, NULL, NULL, 2000)
            """.trimIndent(),
        )

        val migrated = runToVersionFour()

        migrated.query("SELECT createdAt, updatedAt FROM install_progress").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2_000L, cursor.getLong(0))
            assertEquals(2_000L, cursor.getLong(1))
        }
    }

    @Test
    fun migratingToVersionFourKeepsInFlightProgress() {
        seedVersionThree(
            """
            INSERT INTO install_progress
            (githubRepoId, slug, title, iconUrl, status, versionTag, bytesDownloaded,
             bytesTotal, failureReason, failureMessage, updatedAt)
            VALUES (5, 'aurora', 'Aurora', NULL, 'downloading', 'v4', 500, 1000, NULL, NULL, 2000)
            """.trimIndent(),
        )

        val migrated = runToVersionFour()

        migrated.query(
            "SELECT status, bytesDownloaded, bytesTotal FROM install_progress",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("downloading", cursor.getString(0))
            assertEquals(500L, cursor.getLong(1))
            assertEquals(1_000L, cursor.getLong(2))
        }
    }

    @Test
    fun migratingToVersionFourOrdersProgressByWhenEachInstallStarted() {
        seedVersionThree(
            """
            INSERT INTO install_progress
            (githubRepoId, slug, title, iconUrl, status, versionTag, bytesDownloaded,
             bytesTotal, failureReason, failureMessage, updatedAt)
            VALUES (5, 'aurora', 'Aurora', NULL, 'downloading', 'v4', 500, 1000, NULL, NULL, 9000)
            """.trimIndent(),
            """
            INSERT INTO install_progress
            (githubRepoId, slug, title, iconUrl, status, versionTag, bytesDownloaded,
             bytesTotal, failureReason, failureMessage, updatedAt)
            VALUES (6, 'termux', 'Termux', NULL, 'downloading', 'v1', 100, 1000, NULL, NULL, 1000)
            """.trimIndent(),
        )

        val migrated = runToVersionFour()

        migrated.query(
            "SELECT githubRepoId FROM install_progress ORDER BY createdAt ASC, githubRepoId ASC",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(6L, cursor.getLong(0))
            assertTrue(cursor.moveToNext())
            assertEquals(5L, cursor.getLong(0))
        }
    }

    private fun seedVersionTwo(vararg statements: String) {
        helper.createDatabase(TEST_DATABASE, 2).use { database ->
            statements.forEach(database::execSQL)
        }
    }

    private fun seedVersionThree(vararg statements: String) {
        helper.createDatabase(TEST_DATABASE, 3).use { database ->
            statements.forEach(database::execSQL)
        }
    }

    private fun runToVersionThree() =
        helper.runMigrationsAndValidate(TEST_DATABASE, 3, true, MIGRATION_2_TO_3)

    private fun runToVersionFour() =
        helper.runMigrationsAndValidate(TEST_DATABASE, 4, true, MIGRATION_3_TO_4)

    private fun seedVersionFour(vararg statements: String) {
        helper.createDatabase(TEST_DATABASE, 4).use { database ->
            statements.forEach(database::execSQL)
        }
    }

    private fun runToVersionFive() =
        helper.runMigrationsAndValidate(TEST_DATABASE, 5, true, MIGRATION_4_TO_5)

    @Test
    fun migratingToVersionFiveKeepsRecordedInstalls() {
        seedVersionFour(
            """
            INSERT INTO installs
            (githubRepoId, packageName, slug, title, iconUrl, versionTag, versionCode, installedAt)
            VALUES (7, 'com.termux', 'termux', 'Termux', NULL, 'v0.118.0', 118, 1000)
            """.trimIndent(),
        )

        val migrated = runToVersionFive()

        migrated.query("SELECT packageName, versionTag FROM installs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("com.termux", cursor.getString(0))
            assertEquals("v0.118.0", cursor.getString(1))
        }
    }

    @Test
    fun migratingToVersionFiveCreditsExistingInstallsToYuki() {
        seedVersionFour(
            """
            INSERT INTO installs
            (githubRepoId, packageName, slug, title, iconUrl, versionTag, versionCode, installedAt)
            VALUES (7, 'com.termux', 'termux', 'Termux', NULL, 'v0.118.0', 118, 1000)
            """.trimIndent(),
        )

        val migrated = runToVersionFive()

        migrated.query("SELECT source FROM installs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("YUKI", cursor.getString(0))
        }
    }

    @Test
    fun migratingToVersionFiveAddsAnEmptyPackageIndex() {
        helper.createDatabase(TEST_DATABASE, 4).close()

        val migrated = runToVersionFive()

        migrated.query("SELECT COUNT(*) FROM package_index").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun theMigratedPackageIndexAcceptsAnEntry() {
        helper.createDatabase(TEST_DATABASE, 4).close()

        val migrated = runToVersionFive()
        migrated.execSQL(
            """
            INSERT INTO package_index (packageName, githubRepoId, slug, title, iconUrl)
            VALUES ('dev.imranr.obtainium.fdroid', 42, 'obtainium', 'Obtainium', NULL)
            """.trimIndent(),
        )

        migrated.query("SELECT githubRepoId, title FROM package_index").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(42L, cursor.getLong(0))
            assertEquals("Obtainium", cursor.getString(1))
        }
    }

    @Test
    fun migratingToVersionSixKeepsInFlightProgress() {
        seedVersionFive(
            """
            INSERT INTO install_progress
            (githubRepoId, slug, title, iconUrl, status, versionTag, bytesDownloaded,
             bytesTotal, failureReason, failureMessage, createdAt, updatedAt)
            VALUES (5, 'aurora', 'Aurora', NULL, 'failed', 'v4', 0, 0,
             'download_failed', NULL, 1000, 2000)
            """.trimIndent(),
        )

        val migrated = runToVersionSix()

        migrated.query(
            "SELECT status, failureReason, failureCode FROM install_progress",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("failed", cursor.getString(0))
            assertEquals("download_failed", cursor.getString(1))
            assertTrue(cursor.isNull(2))
        }
    }

    @Test
    fun theVersionSixProgressTableAcceptsAFailureCode() {
        helper.createDatabase(TEST_DATABASE, 5).close()

        val migrated = runToVersionSix()
        migrated.execSQL(
            """
            INSERT INTO install_progress
            (githubRepoId, slug, title, iconUrl, status, versionTag, bytesDownloaded,
             bytesTotal, failureReason, failureMessage, failureCode, createdAt, updatedAt)
            VALUES (5, 'aurora', 'Aurora', NULL, 'failed', 'v4', 0, 0,
             'download_failed', NULL, 404, 1000, 2000)
            """.trimIndent(),
        )

        migrated.query("SELECT failureCode FROM install_progress").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(404, cursor.getInt(0))
        }
    }

    private fun seedVersionFive(vararg statements: String) {
        helper.createDatabase(TEST_DATABASE, 5).use { database ->
            statements.forEach(database::execSQL)
        }
    }

    private fun runToVersionSix() = helper.runMigrationsAndValidate(TEST_DATABASE, 6, true)
}
