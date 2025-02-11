package xyz.malkki.neostumbler.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportDatabaseMigrationTest {
    private val TEST_DB = "report-db-migration-test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            ReportDatabase::class.java,
        )

    @Test
    fun testRunningAllMigrations() {
        helper.createDatabase(TEST_DB, 1).apply { close() }

        helper.runMigrationsAndValidate(TEST_DB, REPORT_DB_VERSION, true)
    }
}
