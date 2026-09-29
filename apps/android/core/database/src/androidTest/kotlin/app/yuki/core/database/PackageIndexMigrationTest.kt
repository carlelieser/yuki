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

private const val INDEX_MIGRATION_DATABASE = "yuki-package-index-migration.db"

@RunWith(AndroidJUnit4::class)
class PackageIndexMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        YukiDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migratingToVersionEightKeepsRecordedInstalls() {
        helper.createDatabase(INDEX_MIGRATION_DATABASE, 7).use { database ->
            database.execSQL(
                """
                INSERT INTO installs
                (githubRepoId, packageName, slug, title, iconUrl, versionTag, versionCode,
                 installedAt, source)
                VALUES (7, 'com.termux', 'termux', 'Termux', NULL, 'v0.118.0', 118, 1000, 'YUKI')
                """.trimIndent(),
            )
        }

        val migrated = runToVersionEight()

        migrated.query("SELECT packageName FROM installs").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("com.termux", cursor.getString(0))
        }
    }

    @Test
    fun migratingToVersionEightDiscardsThePackageIndexCachedWithoutSigners() {
        helper.createDatabase(INDEX_MIGRATION_DATABASE, 7).use { database ->
            database.execSQL(
                """
                INSERT INTO package_index (packageName, githubRepoId, slug, title, iconUrl)
                VALUES ('com.google.ar.core', 9, 'neko', 'Neko XRManager', NULL)
                """.trimIndent(),
            )
        }

        val migrated = runToVersionEight()

        migrated.query("SELECT COUNT(*) FROM package_index").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun theVersionEightPackageIndexKeepsOnePackageForSeveralListings() {
        helper.createDatabase(INDEX_MIGRATION_DATABASE, 7).close()

        val migrated = runToVersionEight()
        migrated.execSQL(
            """
            INSERT INTO package_index (packageName, githubRepoId, slug, title, iconUrl, identities)
            VALUES ('com.google.ar.core', 9, 'neko', 'Neko', NULL, 'aa|'),
                   ('com.google.ar.core', 10, 'other', 'Other', NULL, 'bb|')
            """.trimIndent(),
        )

        migrated.query("SELECT COUNT(*) FROM package_index").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }
    }

    private fun runToVersionEight() =
        helper.runMigrationsAndValidate(INDEX_MIGRATION_DATABASE, 8, true, MIGRATION_7_TO_8)
}
