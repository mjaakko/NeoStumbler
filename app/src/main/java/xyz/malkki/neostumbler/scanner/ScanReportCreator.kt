package xyz.malkki.neostumbler.scanner

import android.content.Context
import android.location.Location
import android.net.wifi.ScanResult
import androidx.room.withTransaction
import org.altbeacon.beacon.Beacon
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.db.entities.BluetoothBeaconEntity
import xyz.malkki.neostumbler.db.entities.CellTowerEntity
import xyz.malkki.neostumbler.db.entities.Position
import xyz.malkki.neostumbler.db.entities.Report
import xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity
import xyz.malkki.neostumbler.domain.CellTower
import java.time.Instant

class ScanReportCreator(context: Context) {
    private val reportDb = (context.applicationContext as StumblerApplication).reportDb

    suspend fun createReport(
        locationSource: String,
        location: Location,
        wifiScanResults: List<ScanResult>,
        cellTowers: List<CellTower>,
        beacons: List<Beacon>,
        reportTimestamp: Instant = Instant.now()
    ) = reportDb.withTransaction {
        val report = Report(null, reportTimestamp, false, null)
        val reportId = reportDb.reportDao().insert(report)

        val position = Position.createFromLocation(reportId, reportTimestamp, location, locationSource)
        reportDb.positionDao().insert(position)

        val wifiAccessPointEntities = wifiScanResults.map { WifiAccessPointEntity.createFromScanResult(reportId, reportTimestamp, it) }
        reportDb.wifiAccessPointDao().insertAll(*wifiAccessPointEntities.toTypedArray())

        val cellTowerEntities = cellTowers.map { CellTowerEntity.fromCellTower(it, reportTimestamp, reportId) }
        reportDb.cellTowerDao().insertAll(*cellTowerEntities.toTypedArray())

        val bluetoothBeaconEntities = beacons.map { BluetoothBeaconEntity.fromBeacon(reportId, reportTimestamp, it) }
        reportDb.bluetoothBeaconDao().insertAll(*bluetoothBeaconEntities.toTypedArray())

        Timber.i("Inserted report with ${wifiAccessPointEntities.size} Wi-Fi access points, ${cellTowerEntities.size} cell towers and ${bluetoothBeaconEntities.size} Bluetooth beacons to DB")
    }
}