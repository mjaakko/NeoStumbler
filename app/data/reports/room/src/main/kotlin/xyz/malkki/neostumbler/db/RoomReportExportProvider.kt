package xyz.malkki.neostumbler.db

import android.database.Cursor
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.util.Locale
import xyz.malkki.neostumbler.data.reports.CsvExportCursor
import xyz.malkki.neostumbler.data.reports.ReportExportProvider

class RoomReportExportProvider(private val reportDatabaseManager: ReportDatabaseManager) :
    ReportExportProvider {
    override fun getWifiCursor(from: Instant, to: Instant): CsvExportCursor {
        return AndroidCursorWrapper(
            reportDatabaseManager.reportDb.value.exportDao().wifiExportCursor(from, to)
        )
    }

    override fun getBluetoothCursor(from: Instant, to: Instant): CsvExportCursor {
        return AndroidCursorWrapper(
            reportDatabaseManager.reportDb.value.exportDao().bluetoothExportCursor(from, to)
        )
    }

    override fun getCellCursor(from: Instant, to: Instant): CsvExportCursor {
        return AndroidCursorWrapper(
            reportDatabaseManager.reportDb.value.exportDao().cellExportCursor(from, to)
        )
    }
}

private const val MAXIMUM_FRACTION_DIGITS = 7

private class AndroidCursorWrapper(private val cursor: Cursor) : CsvExportCursor {
    companion object {
        private val DECIMAL_FORMAT =
            DecimalFormat("0", DecimalFormatSymbols(Locale.ROOT)).apply {
                roundingMode = RoundingMode.HALF_UP
                maximumFractionDigits = MAXIMUM_FRACTION_DIGITS
            }
    }

    override fun getHeader(): List<String> {
        return (0 until cursor.columnCount).map { cursor.getColumnName(it) }
    }

    override fun moveToNextRow(): Boolean {
        return cursor.moveToNext()
    }

    override val rowCount: Int
        get() = cursor.count

    override fun getRow(): List<String> {
        return (0 until cursor.columnCount).map { cursor.getCsvValue(it).sanitizeNullString() }
    }

    private fun Cursor.getCsvValue(columnIndex: Int): String {
        return when (getType(columnIndex)) {
            Cursor.FIELD_TYPE_STRING -> getString(columnIndex)
            Cursor.FIELD_TYPE_FLOAT -> DECIMAL_FORMAT.format(getDouble(columnIndex))
            Cursor.FIELD_TYPE_INTEGER -> getLong(columnIndex).toString()
            Cursor.FIELD_TYPE_NULL -> ""
            // Right now we don't have any blobs in the DB
            else -> throw IllegalArgumentException("Invalid type")
        }
    }

    /** Replaces strings containing only null characters (possibly in quotes) with empty strings */
    private fun String.sanitizeNullString(): String {
        val strToCheck =
            if (isNotEmpty() && get(0) == '\"' && get(length - 1) == '\"') {
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

    override fun close() {
        cursor.close()
    }
}
