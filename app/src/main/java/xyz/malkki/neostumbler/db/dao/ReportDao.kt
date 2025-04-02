package xyz.malkki.neostumbler.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import xyz.malkki.neostumbler.db.entities.Report
import xyz.malkki.neostumbler.db.entities.ReportWithData
import xyz.malkki.neostumbler.db.entities.ReportWithLocation
import xyz.malkki.neostumbler.db.entities.ReportWithStats

@Dao
interface ReportDao {
    @Insert suspend fun insert(report: Report): Long

    @Update suspend fun update(vararg reports: Report)

    @Query("DELETE FROM Report WHERE id IN (:reportIds)")
    suspend fun delete(vararg reportIds: Long): Int

    @Query("DELETE FROM Report WHERE timestamp >= :minTimestamp AND timestamp <= :maxTimestamp")
    suspend fun deleteFromTimeRange(minTimestamp: Instant, maxTimestamp: Instant): Int

    @Query("DELETE FROM Report WHERE timestamp <= :timestamp")
    suspend fun deleteOlderThan(timestamp: Instant): Int

    @Query("SELECT COUNT(*) FROM Report") fun getReportCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM Report WHERE uploaded = 0")
    fun getReportCountNotUploaded(): Flow<Int>

    @Query("SELECT MAX(r.uploadTimestamp) AS timestamp FROM Report r WHERE r.uploaded = 1")
    fun getLastUploadTime(): Flow<Instant?>

    @Transaction
    @Query(
        """
        SELECT
            r.id AS reportId,
            r.timestamp AS timestamp,
            p.latitude AS latitude,
            p.longitude AS longitude,
            COALESCE(wap.wifiAccessPointCount, 0) AS wifiAccessPointCount,
            COALESCE(ct.cellTowerCount, 0) AS cellTowerCount,
            COALESCE(bb.bluetoothBeaconCount, 0) AS bluetoothBeaconCount
        FROM Report r, PositionEntity p
        LEFT JOIN (SELECT reportId, COUNT(id) AS wifiAccessPointCount FROM WifiAccessPointEntity GROUP BY reportId) AS wap ON wap.reportId = r.id
        LEFT JOIN (SELECT reportId, COUNT(id) AS cellTowerCount FROM CellTowerEntity GROUP BY reportId) AS ct ON ct.reportId = r.id
        LEFT JOIN (SELECT reportId, COUNT(id) AS bluetoothBeaconCount FROM BluetoothBeaconEntity GROUP BY reportId) AS bb ON bb.reportId = r.id
        WHERE r.uploaded = 0 
        AND r.id = p.reportId
        GROUP BY r.id
        ORDER BY r.timestamp DESC
    """
    )
    fun getAllReportsWithStats(): PagingSource<Int, ReportWithStats>

    @Transaction
    @Query("SELECT * FROM Report WHERE uploaded = 0 ORDER BY timestamp ASC LIMIT :count")
    suspend fun getNotUploadedReports(count: Int): List<ReportWithData>

    @Transaction
    @Query("SELECT * FROM Report WHERE uploaded = 0 ORDER BY RANDOM() ASC LIMIT :count")
    suspend fun getRandomNotUploadedReports(count: Int): List<ReportWithData>

    @Transaction
    @Query("SELECT * FROM Report WHERE id = :reportId")
    fun getReport(reportId: Long): Flow<ReportWithData>

    @Transaction
    @Query("SELECT * FROM Report WHERE timestamp >= :from AND timestamp <= :to")
    suspend fun getAllReportsForTimerange(from: Instant, to: Instant): List<ReportWithData>

    @Query(
        """
        SELECT
            r.id,
            r.timestamp,
            p.latitude,
            p.longitude
        FROM Report r
            JOIN PositionEntity p ON r.id = p.reportId
        WHERE r.timestamp >= :timestamp"""
    )
    suspend fun getReportsNewerThan(timestamp: Instant): List<ReportWithLocation>

    @Transaction
    @Query(
        "SELECT r.id, p.latitude, p.longitude FROM Report r JOIN PositionEntity p ON r.id = p.reportId"
    )
    fun getAllReportsWithLocation(): Flow<List<ReportWithLocation>>

    /**
     * Note that this function does not handle crossing the 180th meridian. For that, use
     * [ReportDao.getReportsInsideBoundingBox]
     */
    @Transaction
    @Query(
        """
        SELECT
            r.id, p.latitude, p.longitude
        FROM Report r
        JOIN PositionEntity p ON r.id = p.reportId
        WHERE p.latitude >= :minLatitude
            AND p.latitude <= :maxLatitude
            AND p.longitude >= :minLongitude
            AND p.longitude <= :maxLongitude
    """
    )
    fun getAllReportsWithLocationInsideBoundingBox(
        minLatitude: Double,
        minLongitude: Double,
        maxLatitude: Double,
        maxLongitude: Double,
    ): Flow<List<ReportWithLocation>>

    @Query("SELECT DISTINCT DATE(ROUND(r.timestamp / 1000), 'unixepoch') FROM Report r")
    fun getReportDates(): Flow<List<LocalDate>>
}

fun ReportDao.getReportsInsideBoundingBox(
    minLatitude: Double,
    minLongitude: Double,
    maxLatitude: Double,
    maxLongitude: Double,
): Flow<List<ReportWithLocation>> {
    return if (minLongitude > maxLongitude) {
        // Handle crossing the 180th meridian
        val left =
            getAllReportsWithLocationInsideBoundingBox(
                minLatitude = minLatitude,
                minLongitude = minLongitude,
                maxLatitude = maxLatitude,
                maxLongitude = 180.0,
            )
        val right =
            getAllReportsWithLocationInsideBoundingBox(
                minLatitude = -180.0,
                minLongitude = minLongitude,
                maxLatitude = maxLatitude,
                maxLongitude = maxLongitude,
            )

        left.combine(right) { listA, listB -> listA + listB }
    } else {
        getAllReportsWithLocationInsideBoundingBox(
            minLatitude = minLatitude,
            minLongitude = minLongitude,
            maxLatitude = maxLatitude,
            maxLongitude = maxLongitude,
        )
    }
}
