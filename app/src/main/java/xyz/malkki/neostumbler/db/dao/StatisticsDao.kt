package xyz.malkki.neostumbler.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import java.time.LocalDate

@Dao
interface StatisticsDao {
    @Query("""
        SELECT date, COUNT(mac) AS count FROM (SELECT wap.macAddress AS mac, MIN(DATE(ROUND(r.timestamp / 1000), 'unixepoch')) AS date FROM WifiAccessPoint wap JOIN Report r ON wap.reportId = r.id GROUP BY wap.macAddress) GROUP BY date
    """)
    fun newWifisPerDay(): LiveData<Map<@MapColumn("date") LocalDate, @MapColumn("count") Long>>

    @Query("""
        SELECT date, COUNT(mac) AS count FROM (SELECT bb.macAddress AS mac, MIN(DATE(ROUND(r.timestamp / 1000), 'unixepoch')) AS date FROM BluetoothBeacon bb JOIN Report r ON bb.reportId = r.id GROUP BY bb.macAddress) GROUP BY date
    """)
    fun newBeaconsPerDay(): LiveData<Map<@MapColumn("date") LocalDate, @MapColumn("count") Long>>

    @Query("""
        SELECT date, COUNT(*) AS count FROM (SELECT radioType, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, primaryScramblingCode, MIN(DATE(ROUND(r.timestamp / 1000), 'unixepoch')) AS date FROM CellTower ct JOIN Report r ON ct.reportId = r.id GROUP BY radioType, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, primaryScramblingCode) GROUP BY date
    """)
    fun newCellsPerDay(): LiveData<Map<@MapColumn("date") LocalDate, @MapColumn("count") Long>>

    @Query("""
        SELECT COUNT(DISTINCT macAddress) FROM WifiAccessPoint
    """)
    fun distinctWifisCount(): LiveData<Long>

    @Query("""
        SELECT COUNT(DISTINCT macAddress) FROM BluetoothBeacon
    """)
    fun distinctBeaconsCount(): LiveData<Long>

    @Query("""
        SELECT COUNT(*) FROM (SELECT DISTINCT radioType, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, primaryScramblingCode FROM CellTower)
    """)
    fun distinctCellsCount(): LiveData<Long>
}