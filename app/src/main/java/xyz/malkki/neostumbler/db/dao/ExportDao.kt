package xyz.malkki.neostumbler.db.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Query

@Dao
interface ExportDao {
    @Query("""
        SELECT
            r.timestamp AS timestamp,
            p.latitude AS latitude,
            p.longitude AS longitude,
            p.accuracy AS locationAccuracy,
            p.age AS locationAge,
            p.speed AS speed,
            b.macAddress AS macAddress,
            b.age AS bluetoothScanAge,
            b.signalStrength AS signalStrength
        FROM Report r
        JOIN Position p ON r.id = p.reportId
        JOIN BluetoothBeacon b ON r.id = b.reportId
    """)
    fun bluetoothExportCursor(): Cursor

    @Query("""
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
        JOIN WifiAccessPoint w ON r.id = w.reportId
    """)
    fun wifiExportCursor(): Cursor

    @Query("""
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
            c.age AS cellScanAge
        FROM Report r
        JOIN Position p ON r.id = p.reportId
        JOIN CellTower c ON r.id = c.reportId
    """)
    fun cellExportCursor(): Cursor
}