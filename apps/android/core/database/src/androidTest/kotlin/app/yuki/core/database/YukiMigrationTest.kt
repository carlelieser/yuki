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

    private fun seedVersionTwo(vararg statements: String) {
        helper.createDatabase(TEST_DATABASE, 2).use { database ->
            statements.forEach(database::execSQL)
        }
    }

    private fun runToVersionThree() =
        helper.runMigrationsAndValidate(TEST_DATABASE, 3, true, MIGRATION_2_TO_3)
}