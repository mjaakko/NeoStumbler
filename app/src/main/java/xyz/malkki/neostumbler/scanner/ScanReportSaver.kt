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
import xyz.malkki.neostumbler.scanner.data.ReportData

class ScanReportSaver(reportDatabaseManager: ReportDatabaseManager) {
    private val reportDb = reportDatabaseManager.reportDb

    suspend fun saveReport(reportData: ReportData, reportTimestamp: Instant = Instant.now()) =
        reportDb.value.let { db ->
            db.withTransaction {
                val report = Report(id = 0, reportTimestamp, false, null)
                val reportId = db.reportDao().insert(report)

                val positionEntity =
                    PositionEntity.createFromPosition(
                        reportId,
                        reportTimestamp,
                        reportData.position,
                    )
                db.positionDao().insert(positionEntity)

                val wifiAccessPointEntities =
                    reportData.wifiAccessPoints.map {
                        WifiAccessPointEntity.fromWifiAccessPoint(it, reportTimestamp, reportId)
                    }
                db.wifiAccessPointDao().insertAll(*wifiAccessPointEntities.toTypedArray())

                val cellTowerEntities =
                    reportData.cellTowers.map {
                        CellTowerEntity.fromCellTower(it, reportTimestamp, reportId)
                    }
                db.cellTowerDao().insertAll(*cellTowerEntities.toTypedArray())

                val bluetoothBeaconEntities =
                    reportData.bluetoothBeacons.map {
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
