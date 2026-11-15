package xyz.malkki.neostumbler.export

import de.siegmar.fastcsv.writer.CsvWriter
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.malkki.neostumbler.data.reports.ReportExportProvider
import xyz.malkki.neostumbler.utils.io.closeShielded

/** Helper for exporting scan data as CSV files */
class CsvExporter(private val reportExportProvider: ReportExportProvider) {
    companion object {
        private const val BEACONS_FILE_NAME = "beacons.csv"
        private const val WIFIS_FILE_NAME = "wifis.csv"
        private const val CELLS_FILE_NAME = "cells.csv"
    }

    suspend fun exportToOutputStream(
        outputStream: OutputStream,
        from: Instant,
        to: Instant,
        /** Progress callback, with number of rows written and total number of rows */
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ) =
        withContext(Dispatchers.IO) {
            reportExportProvider.getWifiCursor(from, to).use { wifiCursor ->
                reportExportProvider.getCellCursor(from, to).use { cellCursor ->
                    reportExportProvider.getBluetoothCursor(from, to).use { bluetoothCursor ->
                        val totalRows =
                            wifiCursor.rowCount + cellCursor.rowCount + bluetoothCursor.rowCount

                        var rowsWritten = 0

                        ZipOutputStream(outputStream.buffered(), StandardCharsets.UTF_8).use {
                            zipOutputStream ->
                            listOf(
                                    WIFIS_FILE_NAME to wifiCursor,
                                    CELLS_FILE_NAME to cellCursor,
                                    BEACONS_FILE_NAME to bluetoothCursor,
                                )
                                .forEach { (fileName, cursor) ->
                                    zipOutputStream.putNextEntry(ZipEntry(fileName))

                                    CsvWriter.builder()
                                        .build(zipOutputStream.closeShielded())
                                        .use { csvWriter ->
                                            csvWriter.writeRecord(cursor.getHeader())
                                            cursor.use {
                                                while (cursor.moveToNextRow()) {
                                                    csvWriter.writeRecord(cursor.getRow())
                                                    rowsWritten++

                                                    onProgress(rowsWritten, totalRows)
                                                }
                                            }
                                        }

                                    zipOutputStream.closeEntry()
                                }
                        }
                    }
                }
            }
        }
}
