package xyz.malkki.neostumbler.db.dao

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface StatisticsDao {
    @Query(
        """
        SELECT date, COUNT(mac) AS count FROM (SELECT wap.macAddress AS mac, MIN(DATE(ROUND(r.timestamp / 1000), 'unixepoch')) AS date FROM WifiAccessPointEntity wap JOIN Report r ON wap.reportId = r.id GROUP BY wap.macAddress) GROUP BY date
    """
    )
    fun newWifisPerDay(): Flow<Map<@MapColumn("date") LocalDate, @MapColumn("count") Long>>

    @Query(
        """
        SELECT date, COUNT(mac) AS count FROM (SELECT bb.macAddress AS mac, MIN(DATE(ROUND(r.timestamp / 1000), 'unixepoch')) AS date FROM BluetoothBeaconEntity bb JOIN Report r ON bb.reportId = r.id GROUP BY bb.macAddress) GROUP BY date
    """
    )
    fun newBeaconsPerDay(): Flow<Map<@MapColumn("date") LocalDate, @MapColumn("count") Long>>

    @Query(
        """
        SELECT date, COUNT(*) AS count FROM (SELECT radioType, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, primaryScramblingCode, MIN(DATE(ROUND(r.timestamp / 1000), 'unixepoch')) AS date FROM CellTowerEntity ct JOIN Report r ON ct.reportId = r.id GROUP BY radioType, mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, primaryScramblingCode) GROUP BY date
    """
    )
    fun newCellsPerDay(): Flow<Map<@MapColumn("date") LocalDate, @MapColumn("count") Long>>
}