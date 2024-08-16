package xyz.malkki.neostumbler.scanner

import android.content.Context
import android.location.Location
import androidx.room.withTransaction
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.db.entities.BluetoothBeaconEntity
import xyz.malkki.neostumbler.db.entities.CellTowerEntity
import xyz.malkki.neostumbler.db.entities.Position
import xyz.malkki.neostumbler.db.entities.Report
import xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity
import xyz.malkki.neostumbler.domain.BluetoothBeacon
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import java.time.Instant

class ScanReportCreator(context: Context) {
    private val reportDb = (context.applicationContext as StumblerApplication).reportDb

    suspend fun createReport(
        locationSource: String,
        location: Location,
        wifiScanResults: List<WifiAccessPoint>,
        cellTowers: List<CellTower>,
        beacons: List<BluetoothBeacon>,
        reportTimestamp: Instant = Instant.now()
    ) = reportDb.withTransaction {
        val report = Report(null, reportTimestamp, false, null)
        val reportId = reportDb.reportDao().insert(report)

        val position = Position.createFromLocation(reportId, reportTimestamp, location, locationSource)
        reportDb.positionDao().insert(position)

        val wifiAccessPointEntities = wifiScanResults.map { WifiAccessPointEntity.fromWifiAccessPoint(it, reportTimestamp, reportId) }
        reportDb.wifiAccessPointDao().insertAll(*wifiAccessPointEntities.toTypedArray())

        val cellTowerEntities = cellTowers.map { CellTowerEntity.fromCellTower(it, reportTimestamp, reportId) }
        reportDb.cellTowerDao().insertAll(*cellTowerEntities.toTypedArray())

        val bluetoothBeaconEntities = beacons.map { BluetoothBeaconEntity.fromBluetoothBeacon(reportId, reportTimestamp, it) }
        reportDb.bluetoothBeaconDao().insertAll(*bluetoothBeaconEntities.toTypedArray())

        Timber.i("Inserted report with ${wifiAccessPointEntities.size} Wi-Fi access points, ${cellTowerEntities.size} cell towers and ${bluetoothBeaconEntities.size} Bluetooth beacons to DB")
    }
}