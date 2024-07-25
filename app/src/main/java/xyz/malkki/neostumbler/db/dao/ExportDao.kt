package xyz.malkki.neostumbler.db.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Query
import java.time.Instant

@Dao
interface ExportDao {
    @Query(
        """
        SELECT
            r.timestamp AS timestamp,
            p.latitude AS latitude,
            p.longitude AS longitude,
            p.accuracy AS locationAccuracy,
            p.age AS locationAge,
            p.speed AS speed,
            b.macAddress AS macAddress,
            b.beaconType AS beaconType,
            b.id1 AS id1,
            b.id2 AS id2,
            b.id3 AS id3,
            b.age AS bluetoothScanAge,
            b.signalStrength AS signalStrength
        FROM Report r
        JOIN Position p ON r.id = p.reportId
        JOIN BluetoothBeaconEntity b ON r.id = b.reportId
        WHERE r.timestamp >= :from AND r.timestamp <= :to
    """
    )
    fun bluetoothExportCursor(from: Instant, to: Instant): Cursor

    @Query(
        """
        SELECT
            r.timestamp AS timestamp,
            p.latitude AS latitude,
            p.longitude AS longitude,
            p.accuracy AS locationAccuracy,
            p.age AS locationAge,
            p.speed AS speed,
            w.macAddress AS macAddress,
            w.age AS wifiScanAge,
            w.signalStrength AS signalStrength,
            w.ssid AS ssid
        FROM Report r
        JOIN Position p ON r.id = p.reportId
        JOIN WifiAccessPointEntity w ON r.id = w.reportId
        WHERE r.timestamp >= :from AND r.timestamp <= :to
    """
    )
    fun wifiExportCursor(from: Instant, to: Instant): Cursor

    @Query(
        """
        SELECT
            r.timestamp AS timestamp,
            p.latitude AS latitude,
            p.longitude AS longitude,
            p.accuracy AS locationAccuracy,
            p.age AS locationAge,
            p.speed AS speed,
            c.cellId AS cellId,
            c.radioType AS radioType,
            c.mobileCountryCode AS mcc,
            c.mobileNetworkCode AS mnc,
            c.locationAreaCode AS lac,
            c.primaryScramblingCode AS psc,
            c.signalStrength AS signalStrength,
            c.asu AS asu,
            c.arfcn AS arfcn,
            c.age AS cellScanAge
        FROM Report r
        JOIN Position p ON r.id = p.reportId
        JOIN CellTowerEntity c ON r.id = c.reportId
        WHERE r.timestamp >= :from AND r.timestamp <= :to
    """
    )
    fun cellExportCursor(from: Instant, to: Instant): Cursor
}