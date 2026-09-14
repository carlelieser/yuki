package app.yuki.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WIRING_DATABASE = "yuki-migration-wiring.db"

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationWiringTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        YukiDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun removeDatabase() {
        context.deleteDatabase(WIRING_DATABASE)
    }

    @Test
    fun openingOverAnExistingInstallDatabaseMigratesRatherThanCrashing() {
        helper.createDatabase(WIRING_DATABASE, 1).close()

        assertEquals(0, progressRowCountAfterOpening())
    }

    @Test
    fun everyShippedSchemaVersionCanBeOpenedByTheRegisteredMigrations() {
        SHIPPED_VERSIONS.forEach { version ->
            context.deleteDatabase(WIRING_DATABASE)
            helper.createDatabase(WIRING_DATABASE, version).close()

            assertEquals(0, progressRowCountAfterOpening())
        }
    }

    @Test
    fun openingAVersionTwoDatabaseReachesTheColumnsTheLibraryNeeds() {
        helper.createDatabase(WIRING_DATABASE, 2).close()

        val database = openDatabase()
        val columns = database.openHelper.writableDatabase
            .query("SELECT * FROM install_progress LIMIT 0")
            .use { cursor -> cursor.columnNames.toList() }

        database.close()

        assertTrue(columns.containsAll(listOf("slug", "title", "iconUrl")))
    }

    @Test
    fun openingAVersionThreeDatabaseReachesTheCreatedAtColumn() {
        helper.createDatabase(WIRING_DATABASE, 3).close()

        val database = openDatabase()
        val columns = database.openHelper.writableDatabase
            .query("SELECT * FROM install_progress LIMIT 0")
            .use { cursor -> cursor.columnNames.toList() }

        database.close()

        assertTrue(columns.contains("createdAt"))
    }

    private fun progressRowCountAfterOpening(): Int {
        val database = openDatabase()
        val rowCount = database.openHelper.writableDatabase
            .query("SELECT count(*) FROM install_progress")
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }

        database.close()

        return rowCount
    }

    private fun openDatabase() =
        Room.databaseBuilder(context, YukiDatabase::class.java, WIRING_DATABASE)
            .addMigrations(*YUKI_MIGRATIONS)
            .build()
}

private val SHIPPED_VERSIONS = listOf(1, 2, 3, 4)
