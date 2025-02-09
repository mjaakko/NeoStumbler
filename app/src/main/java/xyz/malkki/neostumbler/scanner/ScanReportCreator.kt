package xyz.malkki.neostumbler.scanner

import androidx.room.withTransaction
import java.time.Instant
import timber.log.Timber
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.db.entities.BluetoothBeaconEntity
import xyz.malkki.neostumbler.db.entities.CellTowerEntity
import xyz.malkki.neostumbler.db.entities.PositionEntity
import xyz.malkki.neostumbler.db.entities.Report
import xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity
import xyz.malkki.neostumbler.domain.BluetoothBeacon
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.domain.Position
import xyz.malkki.neostumbler.domain.WifiAccessPoint

class ScanReportCreator(private val reportDatabaseManager: ReportDatabaseManager) {
    suspend fun createReport(
        position: Position,
        wifiScanResults: List<WifiAccessPoint>,
        cellTowers: List<CellTower>,
        beacons: List<BluetoothBeacon>,
        reportTimestamp: Instant = Instant.now(),
    ) =
        reportDatabaseManager.reportDb.value.let { db ->
            db.withTransaction {
                val report = Report(null, reportTimestamp, false, null)
                val reportId = db.reportDao().insert(report)

                val positionEntity =
                    PositionEntity.createFromPosition(reportId, reportTimestamp, position)
                db.positionDao().insert(positionEntity)

                val wifiAccessPointEntities =
                    wifiScanResults.map {
                        WifiAccessPointEntity.fromWifiAccessPoint(it, reportTimestamp, reportId)
                    }
                db.wifiAccessPointDao().insertAll(*wifiAccessPointEntities.toTypedArray())

                val cellTowerEntities =
                    cellTowers.map { CellTowerEntity.fromCellTower(it, reportTimestamp, reportId) }
                db.cellTowerDao().insertAll(*cellTowerEntities.toTypedArray())

                val bluetoothBeaconEntities =
                    beacons.map {
                        BluetoothBeaconEntity.fromBluetoothBeacon(reportId, reportTimestamp, it)
                    }
                db.bluetoothBeaconDao().insertAll(*bluetoothBeaconEntities.toTypedArray())

                Timber.i(
                    "Inserted report with %d Wi-Fi access points, %d cell towers and %d Bluetooth beacons to DB",
                    wifiAccessPointEntities.size,
                    cellTowerEntities.size,
                    bluetoothBeaconEntities.size,
                )
            }
        }
}
