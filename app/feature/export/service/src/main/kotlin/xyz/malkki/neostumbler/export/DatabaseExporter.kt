package xyz.malkki.neostumbler.export

import java.io.OutputStream
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.malkki.neostumbler.data.reports.RawReportImportExport

class DatabaseExporter(
    private val tempFileDir: Path,
    private val rawReportImportExport: RawReportImportExport,
) {
    /**
     * @param outputStream Output stream where to write the database
     * @param compress Whether to compress the database file
     */
    suspend fun exportToOutputStream(outputStream: OutputStream, compress: Boolean) {
        val tempFile = createTempFile(tempFileDir, "export", "db")

        try {
            rawReportImportExport.exportRawReports(tempFile)

            val outputStreamWithCompression =
                if (compress) {
                    GZIPOutputStream(outputStream)
                } else {
                    outputStream
                }

            withContext(Dispatchers.IO) {
                tempFile.inputStream().use { input ->
                    outputStreamWithCompression.use { output -> input.copyTo(output) }
                }
            }
        } finally {
            tempFile.deleteIfExists()
        }
    }
}
