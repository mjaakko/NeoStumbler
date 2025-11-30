package xyz.malkki.neostumbler.export

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import xyz.malkki.neostumbler.data.reports.RawReportImportExport

class DatabaseExporterTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `Temporary file is deleted if exporting fails`() = runTest {
        val fakeExportImport =
            object : RawReportImportExport {
                override suspend fun validateRawReports(file: Path): Boolean {
                    return true
                }

                override suspend fun importRawReports(file: Path) {}

                override suspend fun exportRawReports(file: Path) {
                    file.writeText("test_db_content")

                    throw IOException("Oops")
                }
            }

        val tempDir = temporaryFolder.newFolder().toPath()

        val databaseExporter =
            DatabaseExporter(tempFileDir = tempDir, rawReportImportExport = fakeExportImport)

        try {
            databaseExporter.exportToOutputStream(ByteArrayOutputStream(), compress = false)
        } catch (_: IOException) {}

        assertEquals(0, Files.list(tempDir).count())
    }
}
