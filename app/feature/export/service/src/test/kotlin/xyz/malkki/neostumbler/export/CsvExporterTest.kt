package xyz.malkki.neostumbler.export

import java.time.Instant
import java.util.zip.ZipFile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import xyz.malkki.neostumbler.data.reports.CsvExportCursor
import xyz.malkki.neostumbler.data.reports.ReportExportProvider

class CsvExporterTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `zip file contains all CSV files`() = runTest {
        val fakeDataCursorProvider = {
            object : CsvExportCursor {
                private var nextRow = true

                override fun getHeader(): List<String> {
                    return listOf("head_1", "head_2", "head_3")
                }

                override fun moveToNextRow(): Boolean {
                    return nextRow
                }

                override val rowCount: Int
                    get() = 1

                override fun getRow(): List<String> {
                    nextRow = false
                    return listOf("a", "b", "c")
                }

                override fun close() {}
            }
        }

        val fakeReportExportProvider =
            object : ReportExportProvider {
                override fun getWifiCursor(from: Instant, to: Instant): CsvExportCursor {
                    return fakeDataCursorProvider()
                }

                override fun getBluetoothCursor(from: Instant, to: Instant): CsvExportCursor {
                    return fakeDataCursorProvider()
                }

                override fun getCellCursor(from: Instant, to: Instant): CsvExportCursor {
                    return fakeDataCursorProvider()
                }
            }

        val csvExporter = CsvExporter(fakeReportExportProvider)

        val outputZipFile = temporaryFolder.newFile("export.zip")
        outputZipFile.outputStream().buffered().use { outputStream ->
            csvExporter.exportToOutputStream(outputStream, Instant.MIN, Instant.MAX)
        }

        ZipFile(outputZipFile).use { zipFile ->
            assertNotNull(zipFile.getEntry("wifis.csv"))
            assertNotNull(zipFile.getEntry("cells.csv"))
            assertNotNull(zipFile.getEntry("beacons.csv"))
        }
    }
}
