package xyz.malkki.neostumbler.data.reports

import java.time.Instant
import xyz.malkki.neostumbler.geography.LatLng

interface ReportRemover {
    suspend fun deleteAll()

    suspend fun deleteReport(reportId: Long)

    suspend fun deleteOlderThan(maxTimestamp: Instant): Int

    suspend fun deleteByDate(fromTimestamp: Instant, toTimestamp: Instant): Int

    suspend fun deleteFromArea(center: LatLng, radius: Double): Int
}
