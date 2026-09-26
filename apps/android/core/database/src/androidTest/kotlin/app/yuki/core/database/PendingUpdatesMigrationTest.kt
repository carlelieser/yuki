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

private const val TEST_DATABASE = "yuki-pending-updates-migration-test.db"

@RunWith(AndroidJUnit4::class)
class PendingUpdatesMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        YukiDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migratingToVersionSevenKeepsRecordedInstalls() {
        helper.createDatabase(TEST_DATABASE, 6).use { database ->
            database.execSQL(
                """
                INSERT INTO installs
                (githubRepoId, packageName, slug, title, iconUrl, versionTag, versionCode,
                 installedAt, source)
                VALUES (7, 'com.termux', 'termux', 'Termux', NULL, 'v1', 1, 1000, 'YUKI')
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DATABASE, 7, true)

        migrated.query("SELECT packageName, versionTag FROM installs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("com.termux", cursor.getString(0))
            assertEquals("v1", cursor.getString(1))
        }
    }

    @Test
    fun theVersionSevenPendingUpdatesTableAcceptsAnUpdate() {
        helper.createDatabase(TEST_DATABASE, 6).close()

        val migrated = helper.runMigrationsAndValidate(TEST_DATABASE, 7, true)
        migrated.execSQL(
            "INSERT INTO pending_updates (githubRepoId, versionTag, isNotified) VALUES (7, 'v2', 0)",
        )

        migrated.query("SELECT versionTag, isNotified FROM pending_updates").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("v2", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
    }
}
