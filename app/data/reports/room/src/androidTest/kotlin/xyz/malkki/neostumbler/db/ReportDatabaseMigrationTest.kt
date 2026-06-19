package xyz.malkki.neostumbler.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "report-db-migration-test"

@RunWith(AndroidJUnit4::class)
class ReportDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            ReportDatabase::class.java,
        )

    @Test
    fun testRunningAllMigrations() {
        helper.createDatabase(TEST_DB, 1).close()

        helper.runMigrationsAndValidate(TEST_DB, REPORT_DB_VERSION, true)
    }
}
