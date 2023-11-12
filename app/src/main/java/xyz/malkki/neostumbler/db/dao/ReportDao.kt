package xyz.malkki.neostumbler.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import xyz.malkki.neostumbler.db.entities.Report
import xyz.malkki.neostumbler.db.entities.ReportWithData
import xyz.malkki.neostumbler.db.entities.ReportWithLocation
import xyz.malkki.neostumbler.db.entities.ReportWithStats
import java.time.Instant

@Dao
interface ReportDao {
    @Insert
    suspend fun insert(report: Report): Long

    @Update
    suspend fun update(vararg reports: Report)

    @Query("DELETE FROM Report WHERE timestamp <= :timestamp")
    suspend fun deleteOlderThan(timestamp: Instant): Int

    @Query("SELECT COUNT(*) FROM Report")
    fun getReportCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM Report WHERE uploaded = 0")
    fun getReportCountNotUploaded(): LiveData<Int>

    @Query("SELECT MAX(r.uploadTimestamp) AS timestamp FROM Report r WHERE r.uploaded = 1")
    fun getLastUploadTime(): LiveData<Instant>

    /*@Transaction
    @Query("SELECT * FROM Report")
    suspend fun getAllReports(): List<ReportWithWifiAccessPoints>*/

    @Transaction
    @Query("""
        SELECT
            r.id AS reportId,
            r.timestamp AS timestamp,
            p.latitude AS latitude,
            p.longitude AS longitude,
            COALESCE(wap.wifiAccessPointCount, 0) AS wifiAccessPointCount,
            COALESCE(ct.cellTowerCount, 0) AS cellTowerCount,
            COALESCE(bb.bluetoothBeaconCount, 0) AS bluetoothBeaconCount
        FROM Report r, Position p
        LEFT JOIN (SELECT reportId, COUNT(id) AS wifiAccessPointCount FROM WifiAccessPoint GROUP BY reportId) AS wap ON wap.reportId = r.id
        LEFT JOIN (SELECT reportId, COUNT(id) AS cellTowerCount FROM CellTower GROUP BY reportId) AS ct ON ct.reportId = r.id
        LEFT JOIN (SELECT reportId, COUNT(id) AS bluetoothBeaconCount FROM BluetoothBeacon GROUP BY reportId) AS bb ON bb.reportId = r.id
        WHERE r.uploaded = 0 
        AND r.id = p.reportId
        GROUP BY r.id
        ORDER BY r.timestamp DESC
    """)
    fun getAllReportsWithWifiAccessPointCount(): LiveData<List<ReportWithStats>>

    @Transaction
    @Query("SELECT * FROM Report WHERE uploaded = 0")
    suspend fun getAllReportsNotUploaded(): List<ReportWithData>

    @Query("SELECT r.id, r.timestamp, p.latitude, p.longitude FROM Report r JOIN Position p ON r.id = p.reportId WHERE r.timestamp >= :timestamp")
    suspend fun getReportsNewerThan(timestamp: Instant): List<ReportWithLocation>

    @Transaction
    @Query("SELECT r.id, r.timestamp, p.latitude, p.longitude FROM Report r JOIN Position p ON r.id = p.reportId")
    fun getAllReportsWithLocation(): LiveData<List<ReportWithLocation>>
}