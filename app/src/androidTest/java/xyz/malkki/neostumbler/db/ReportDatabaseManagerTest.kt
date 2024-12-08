package xyz.malkki.neostumbler.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.nio.file.Path
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class ReportDatabaseManagerTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    private lateinit var context: Context

    private lateinit var dbFile: Path

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()

        dbFile = tmpFolder.newFile("reports.db").toPath()
    }

    @Test
    fun testValidatingValidDatabaseFile() = runTest {
        InstrumentationRegistry.getInstrumentation().context.assets.open("valid_reports.db")
            .use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

        val isValid = ReportDatabaseManager.validateDatabase(context, dbFile)

        assertTrue(isValid)
    }

    @Test
    fun testValidatingInvalidDatabaseFile() = runTest {
        dbFile.writeBytes(Random.Default.nextBytes(1024))

        val isValid = ReportDatabaseManager.validateDatabase(context, dbFile)

        assertFalse(isValid)
    }
}