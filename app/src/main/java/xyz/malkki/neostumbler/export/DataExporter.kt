package xyz.malkki.neostumbler.export

import android.database.Cursor
import android.net.Uri
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Helper for exporting scan data
 */
class DataExporter(private val application: StumblerApplication) {
    companion object {
        private const val BEACONS_FILE_NAME = "beacons.csv"
        private const val WIFIS_FILE_NAME = "wifis.csv"
        private const val CELLS_FILE_NAME = "cells.csv"
    }

    private val exportDao = application.reportDb.exportDao()

    /**
     * Exports data from the given cursor to the ZIP output stream to an entry with the specified file name
     */
    private fun export(zipOutputStream: ZipOutputStream, fileName: String, cursor: Cursor) {
        cursor.use {
            if (cursor.count > 0) {
                zipOutputStream.putNextEntry(ZipEntry(fileName))

                val csvHeader = (0 until cursor.columnCount).map { cursor.getColumnName(it) }.toTypedArray()
                val csvFormat = CSVFormat.Builder.create(CSVFormat.RFC4180).setHeader(*csvHeader).setSkipHeaderRecord(false).build()

                CSVPrinter(OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8), csvFormat).use { csvPrinter ->
                    while (cursor.moveToNext()) {
                        val csvRecord = (0 until cursor.columnCount).map {
                            cursor.getStringOrNull(it)
                        }

                        csvPrinter.printRecord(csvRecord)
                    }
                }

                zipOutputStream.closeEntry()
            }
        }
    }

    private suspend fun exportToOutputStream(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        ZipOutputStream(outputStream.buffered(), StandardCharsets.UTF_8).use { zipOutputStream ->
            export(zipOutputStream, BEACONS_FILE_NAME, exportDao.bluetoothExportCursor(Instant.MIN, Instant.MAX))

            export(zipOutputStream, CELLS_FILE_NAME, exportDao.cellExportCursor(Instant.MIN, Instant.MAX))

            export(zipOutputStream, WIFIS_FILE_NAME, exportDao.wifiExportCursor(Instant.MIN, Instant.MAX))
        }
    }

    /**
     * Exports data to the specified URI (content://)
     */
    suspend fun exportToFile(uri: Uri, from: Instant, to: Instant) {
        application.contentResolver.openOutputStream(uri, "wt").use { os ->
            if (os == null) {
                Timber.w("OutputStream was null, maybe the content provider handling %s crashed", uri.toString())

                throw IOException("OutputStream was null")
            }

            exportToOutputStream(os)
        }
    }
}