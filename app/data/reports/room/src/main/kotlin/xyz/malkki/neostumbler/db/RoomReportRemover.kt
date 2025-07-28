package xyz.malkki.neostumbler.db

import androidx.room.withTransaction
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import xyz.malkki.neostumbler.data.reports.ReportRemover
import xyz.malkki.neostumbler.db.dao.getReportsInsideBoundingBox
import xyz.malkki.neostumbler.geography.LatLng

class RoomReportRemover(private val reportDatabaseManager: ReportDatabaseManager) : ReportRemover {
    override suspend fun deleteAll() =
        withContext(Dispatchers.IO) { reportDatabaseManager.reportDb.value.clearAllTables() }

    override suspend fun deleteReport(reportId: Long) {
        reportDatabaseManager.reportDb.value.reportDao().delete(reportId)
    }

    override suspend fun deleteOlderThan(maxTimestamp: Instant): Int {
        return reportDatabaseManager.reportDb.value.reportDao().deleteOlderThan(maxTimestamp)
    }

    override suspend fun deleteByDate(fromTimestamp: Instant, toTimestamp: Instant): Int {
        return reportDatabaseManager.reportDb.value
            .reportDao()
            .deleteFromTimeRange(minTimestamp = fromTimestamp, maxTimestamp = toTimestamp)
    }

    override suspend fun deleteFromArea(center: LatLng, radius: Double): Int {
        // Because we don't have any geographic functions available in SQLite,
        // we can't delete reports directly. Instead, first query reports inside a
        // bounding box and then filter the ones inside the circle
        val (bottomLeft, topRight) = getBoundingBoxForCircle(center, radius)

        val database = reportDatabaseManager.reportDb.value

        return database.withTransaction {
            val reportDao = database.reportDao()

            val reportsInsideBoundingBox =
                reportDao
                    .getReportsInsideBoundingBox(
                        minLatitude = bottomLeft.latitude,
                        minLongitude = bottomLeft.longitude,
                        maxLatitude = topRight.latitude,
                        maxLongitude = topRight.longitude,
                    )
                    .first()

            val reportsToDelete =
                reportsInsideBoundingBox
                    .filter { report ->
                        LatLng(latitude = report.latitude, longitude = report.longitude)
                            .distanceTo(center) <= radius
                    }
                    .map { it.id }
                    .toLongArray()

            reportDao.delete(*reportsToDelete)
        }
    }
}

private fun getBoundingBoxForCircle(center: LatLng, radius: Double): Pair<LatLng, LatLng> {
    val topRight =
        center
            .destination(distance = radius, bearing = 0.0)
            .destination(distance = radius, bearing = 90.0)
    val bottomLeft =
        center
            .destination(distance = radius, bearing = 180.0)
            .destination(distance = radius, bearing = 270.0)

    return bottomLeft to topRight
}
