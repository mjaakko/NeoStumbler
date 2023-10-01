package xyz.malkki.wifiscannerformls.scanner

import android.content.Context
import android.location.Location
import android.net.wifi.ScanResult
import android.telephony.CellInfo
import androidx.room.withTransaction
import org.altbeacon.beacon.Beacon
import timber.log.Timber
import xyz.malkki.wifiscannerformls.WifiScannerApplication
import xyz.malkki.wifiscannerformls.db.entities.BluetoothBeacon
import xyz.malkki.wifiscannerformls.db.entities.CellTower
import xyz.malkki.wifiscannerformls.db.entities.Position
import xyz.malkki.wifiscannerformls.db.entities.Report
import xyz.malkki.wifiscannerformls.db.entities.WifiAccessPoint
import java.time.Instant

class ScanReportCreator(context: Context) {
    private val reportDb = (context.applicationContext as WifiScannerApplication).reportDb

    suspend fun createReport(
        locationSource: String,
        location: Location,
        wifiScanResults: List<ScanResult>,
        cellInfo: List<CellInfo>,
        beacons: List<Beacon>,
        reportTimestamp: Instant = Instant.now()
    ) = reportDb.withTransaction {
        val report = Report(null, reportTimestamp, false, null)
        val reportId = reportDb.reportDao().insert(report)

        val position = Position.createFromLocation(reportId, reportTimestamp, location, locationSource)
        reportDb.positionDao().insert(position)

        val wifiAccessPoints = wifiScanResults.map { WifiAccessPoint.createFromScanResult(reportId, reportTimestamp, it) }
        reportDb.wifiAccessPointDao().insertAll(*wifiAccessPoints.toTypedArray())

        val cellTowers = cellInfo.mapNotNull { CellTower.fromCellInfo(reportId, reportTimestamp, it) }
        reportDb.cellTowerDao().insertAll(*cellTowers.toTypedArray())

        val bluetoothBeacons = beacons.map { BluetoothBeacon.fromBeacon(reportId, reportTimestamp, it) }
        reportDb.bluetoothBeaconDao().insertAll(*bluetoothBeacons.toTypedArray())

        Timber.i("Inserted report with ${wifiAccessPoints.size} Wi-Fi access points, ${cellTowers.size} cell towers and ${bluetoothBeacons.size} Bluetooth beacons to DB")
    }
}