package xyz.malkki.neostumbler.scanner

import android.content.Context
import androidx.room.withTransaction
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.db.entities.BluetoothBeaconEntity
import xyz.malkki.neostumbler.db.entities.CellTowerEntity
import xyz.malkki.neostumbler.db.entities.PositionEntity
import xyz.malkki.neostumbler.db.entities.Report
import xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity
import xyz.malkki.neostumbler.domain.BluetoothBeacon
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.domain.Position
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import java.time.Instant

class ScanReportCreator(context: Context) {
    private val reportDb = (context.applicationContext as StumblerApplication).reportDb

    suspend fun createReport(
        position: Position,
        wifiScanResults: List<WifiAccessPoint>,
        cellTowers: List<CellTower>,
        beacons: List<BluetoothBeacon>,
        reportTimestamp: Instant = Instant.now()
    ) = reportDb.value.let { db ->
        db.withTransaction {
            val report = Report(null, reportTimestamp, false, null)
            val reportId = db.reportDao().insert(report)

            val positionEntity = PositionEntity.createFromPosition(reportId, reportTimestamp, position)
            db.positionDao().insert(positionEntity)

            val wifiAccessPointEntities = wifiScanResults.map { WifiAccessPointEntity.fromWifiAccessPoint(it, reportTimestamp, reportId) }
            db.wifiAccessPointDao().insertAll(*wifiAccessPointEntities.toTypedArray())

            val cellTowerEntities = cellTowers.map { CellTowerEntity.fromCellTower(it, reportTimestamp, reportId) }
            db.cellTowerDao().insertAll(*cellTowerEntities.toTypedArray())

            val bluetoothBeaconEntities = beacons.map { BluetoothBeaconEntity.fromBluetoothBeacon(reportId, reportTimestamp, it) }
            db.bluetoothBeaconDao().insertAll(*bluetoothBeaconEntities.toTypedArray())

            Timber.i("Inserted report with ${wifiAccessPointEntities.size} Wi-Fi access points, ${cellTowerEntities.size} cell towers and ${bluetoothBeaconEntities.size} Bluetooth beacons to DB")
        }
    }
}