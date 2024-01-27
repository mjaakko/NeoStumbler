package xyz.malkki.neostumbler.export

import android.database.Cursor
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.util.Locale
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

    private val decimalFormat = DecimalFormat("0", DecimalFormatSymbols(Locale.ROOT)).apply {
        roundingMode = RoundingMode.HALF_UP
        maximumFractionDigits = 7
    }

    /**
     * Exports data from the given cursor to the ZIP output stream to an entry with the specified file name
     */
    private fun export(zipOutputStream: ZipOutputStream, fileName: String, cursor: Cursor) {
        cursor.use {
            if (cursor.count > 0) {
                zipOutputStream.putNextEntry(ZipEntry(fileName))

                val csvHeader = (0 until cursor.columnCount).map { cursor.getColumnName(it) }.toTypedArray()
                val csvFormat = CSVFormat.Builder.create(CSVFormat.RFC4180).setHeader(*csvHeader).setSkipHeaderRecord(false).build()

                val csvPrinter = CSVPrinter(OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8), csvFormat)
                while (cursor.moveToNext()) {
                    val csvRecord = (0 until cursor.columnCount).map {
                        cursor.getCsvValue(it).sanitizeNullString()
                    }

                    csvPrinter.printRecord(csvRecord)
                }
                //Only flush CSVPrinter. Closing it will close the ZIP output stream and we might still need to write more data to it
                csvPrinter.flush()

                zipOutputStream.closeEntry()
            }
        }
    }

    /**
     * Replaces strings containing only null characters (possibly in quotes) with empty strings
     */
    private fun String.sanitizeNullString(): String {
        val strToCheck = if (isNotEmpty() && get(0) == '\"' && get(length - 1) == '\"') {
            substring(1, length)
        } else {
            this
        }

        return if (strToCheck.all { it == '\u0000' }) {
            ""
        } else {
            this
        }
    }

    private fun Cursor.getCsvValue(columnIndex: Int): String {
        return when (getType(columnIndex)) {
            Cursor.FIELD_TYPE_STRING -> getString(columnIndex)
            Cursor.FIELD_TYPE_FLOAT -> decimalFormat.format(getDouble(columnIndex))
            Cursor.FIELD_TYPE_INTEGER -> getLong(columnIndex).toString()
            Cursor.FIELD_TYPE_NULL -> ""
            //Right now we don't have any blobs in the DB
            else -> throw IllegalArgumentException("Invalid type")
        }
    }

    private suspend fun exportToOutputStream(outputStream: OutputStream, from: Instant, to: Instant) = withContext(Dispatchers.IO) {
        ZipOutputStream(outputStream.buffered(), StandardCharsets.UTF_8).use { zipOutputStream ->
            export(zipOutputStream, BEACONS_FILE_NAME, exportDao.bluetoothExportCursor(from, to))

            export(zipOutputStream, CELLS_FILE_NAME, exportDao.cellExportCursor(from, to))

            export(zipOutputStream, WIFIS_FILE_NAME, exportDao.wifiExportCursor(from, to))
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

            exportToOutputStream(os, from, to)
        }
    }
}