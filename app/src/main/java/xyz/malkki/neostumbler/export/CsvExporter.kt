package xyz.malkki.neostumbler.export

import android.content.Context
import android.net.Uri
import de.siegmar.fastcsv.writer.CsvWriter
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.malkki.neostumbler.data.reports.CsvExportCursor
import xyz.malkki.neostumbler.data.reports.ReportExportProvider
import xyz.malkki.neostumbler.utils.io.closeShielded

/** Helper for exporting scan data as CSV files */
class CsvExporter(
    private val context: Context,
    private val reportExportProvider: ReportExportProvider,
) {
    companion object {
        private const val BEACONS_FILE_NAME = "beacons.csv"
        private const val WIFIS_FILE_NAME = "wifis.csv"
        private const val CELLS_FILE_NAME = "cells.csv"
    }

    /**
     * Exports data from the given cursor to the ZIP output stream to an entry with the specified
     * file name
     */
    private fun export(
        zipOutputStream: ZipOutputStream,
        fileName: String,
        cursor: CsvExportCursor,
    ) {
        cursor.use {
            if (cursor.rowCount > 0) {
                zipOutputStream.putNextEntry(ZipEntry(fileName))

                CsvWriter.builder().build(zipOutputStream.closeShielded()).use { csvWriter ->
                    cursor.writeToCsv(csvWriter)
                }

                zipOutputStream.closeEntry()
            }
        }
    }

    private fun CsvExportCursor.writeToCsv(csvWriter: CsvWriter) {
        csvWriter.writeRecord(getHeader())

        while (moveToNextRow()) {
            csvWriter.writeRecord(getRow())
        }
    }

    private suspend fun exportToOutputStream(
        outputStream: OutputStream,
        from: Instant,
        to: Instant,
    ) =
        withContext(Dispatchers.IO) {
            ZipOutputStream(outputStream.buffered(), StandardCharsets.UTF_8).use { zipOutputStream
                ->
                export(
                    zipOutputStream,
                    BEACONS_FILE_NAME,
                    reportExportProvider.getBluetoothCursor(from, to),
                )

                export(
                    zipOutputStream,
                    CELLS_FILE_NAME,
                    reportExportProvider.getCellCursor(from, to),
                )

                export(
                    zipOutputStream,
                    WIFIS_FILE_NAME,
                    reportExportProvider.getWifiCursor(from, to),
                )
            }
        }

    /** Exports data to the specified URI (content://) */
    suspend fun exportToFile(uri: Uri, from: Instant, to: Instant) {
        context.contentResolver.openOutputStream(uri, "wt").use { os ->
            if (os == null) {
                Timber.w(
                    "OutputStream was null, maybe the content provider handling %s crashed",
                    uri.toString(),
                )

                throw IOException("OutputStream was null")
            }

            exportToOutputStream(os, from, to)
        }
    }
}
