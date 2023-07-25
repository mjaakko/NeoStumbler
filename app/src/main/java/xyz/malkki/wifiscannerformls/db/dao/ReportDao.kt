package xyz.malkki.wifiscannerformls.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import xyz.malkki.wifiscannerformls.db.entities.Report
import xyz.malkki.wifiscannerformls.db.entities.ReportWithLocation
import xyz.malkki.wifiscannerformls.db.entities.ReportWithWifiAccessPointCount
import xyz.malkki.wifiscannerformls.db.entities.ReportWithWifiAccessPoints
import java.time.Instant

@Dao
interface ReportDao {
    @Insert
    suspend fun insert(report: Report): Long

    @Update
    suspend fun update(vararg reports: Report)

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
            COUNT(wap.id) AS wifiAccessPointCount
        FROM Report r, Position p, WifiAccessPoint wap 
        WHERE r.uploaded = 0 
        AND r.id = p.reportId
        AND r.id = wap.reportId
        GROUP BY r.id
        ORDER BY r.timestamp DESC
    """)
    fun getAllReportsWithWifiAccessPointCount(): LiveData<List<ReportWithWifiAccessPointCount>>

    @Transaction
    @Query("SELECT * FROM Report WHERE uploaded = 0")
    suspend fun getAllReportsNotUploaded(): List<ReportWithWifiAccessPoints>

    @Query("SELECT r.id, r.timestamp, p.latitude, p.longitude FROM Report r JOIN Position p ON r.id = p.reportId WHERE r.timestamp >= :timestamp")
    suspend fun getReportsNewerThan(timestamp: Instant): List<ReportWithLocation>
}