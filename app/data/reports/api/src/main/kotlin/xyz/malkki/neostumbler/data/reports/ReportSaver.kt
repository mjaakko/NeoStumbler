package xyz.malkki.neostumbler.data.reports

import java.time.Instant
import xyz.malkki.neostumbler.core.report.ReportData

interface ReportSaver {
    suspend fun createReport(reportData: ReportData)

    suspend fun markAsUploaded(uploadTimestamp: Instant, vararg reportIds: Long)
}
