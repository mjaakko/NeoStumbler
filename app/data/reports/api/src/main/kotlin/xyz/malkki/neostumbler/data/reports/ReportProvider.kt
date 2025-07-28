package xyz.malkki.neostumbler.data.reports

import androidx.paging.PagingData
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.core.report.Report
import xyz.malkki.neostumbler.core.report.ReportWithLocation
import xyz.malkki.neostumbler.core.report.ReportWithStats

@Suppress("TooManyFunctions") // TODO: split to smaller interfaces
interface ReportProvider {
    fun getReportCount(): Flow<Int>

    fun getNotUploadedReportCount(): Flow<Int>

    fun getLastReportUploadTime(): Flow<Instant?>

    fun getReportDates(): Flow<Set<LocalDate>>

    fun getReportsWithStats(): Flow<PagingData<ReportWithStats>>

    fun getReport(reportId: Long): Flow<Report>

    fun getLatestReportLocation(): Flow<ReportWithLocation?>

    suspend fun getNotUploadedReports(count: Int): List<Report>

    suspend fun getRandomNotUploadedReports(count: Int): List<Report>

    suspend fun getReportsForTimerange(fromTimestamp: Instant, toTimestamp: Instant): List<Report>

    suspend fun getReportsNewerThan(timestamp: Instant): List<ReportWithLocation>

    fun getReportsInsideBoundingBox(
        minLatitude: Double,
        minLongitude: Double,
        maxLatitude: Double,
        maxLongitude: Double,
    ): Flow<List<ReportWithLocation>>
}
