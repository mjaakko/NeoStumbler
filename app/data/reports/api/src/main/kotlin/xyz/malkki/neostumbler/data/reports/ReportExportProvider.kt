package xyz.malkki.neostumbler.data.reports

import java.time.Instant

interface ReportExportProvider {
    fun getWifiCursor(from: Instant, to: Instant): CsvExportCursor

    fun getBluetoothCursor(from: Instant, to: Instant): CsvExportCursor

    fun getCellCursor(from: Instant, to: Instant): CsvExportCursor
}

interface CsvExportCursor : AutoCloseable {
    fun getHeader(): List<String>

    fun moveToNextRow(): Boolean

    val rowCount: Int

    fun getRow(): List<String>
}
