package xyz.malkki.neostumbler.db

import android.os.SystemClock
import androidx.room.withTransaction
import java.time.Instant
import timber.log.Timber
import xyz.malkki.neostumbler.core.report.ReportData
import xyz.malkki.neostumbler.data.reports.ReportSaver
import xyz.malkki.neostumbler.db.entities.BluetoothBeaconEntity
import xyz.malkki.neostumbler.db.entities.CellTowerEntity
import xyz.malkki.neostumbler.db.entities.PositionEntity
import xyz.malkki.neostumbler.db.entities.Report
import xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity

class RoomReportSaver(private val reportDatabaseManager: ReportDatabaseManager) : ReportSaver {
    override suspend fun createReport(reportData: ReportData) {
        val reportDatabase = reportDatabaseManager.reportDb.value

        reportDatabase.withTransaction {
            val reportTimestamp =
                Instant.now()
                    .minusMillis(SystemClock.elapsedRealtime() - (reportData.position.timestamp))

            val report = Report(id = 0, reportTimestamp, false, null)
            val reportId = reportDatabase.reportDao().insert(report)

            val positionEntity =
                PositionEntity.createFromPositionObservation(
                    reportId,
                    reportTimestamp,
                    reportData.position,
                )
            reportDatabase.positionDao().insert(positionEntity)

            val wifiAccessPointEntities =
                reportData.wifiAccessPoints.map {
                    WifiAccessPointEntity.fromWifiAccessPoint(it, reportTimestamp, reportId)
                }
            reportDatabase.wifiAccessPointDao().insertAll(*wifiAccessPointEntities.toTypedArray())

            val cellTowerEntities =
                reportData.cellTowers.map {
                    CellTowerEntity.fromCellTower(it, reportTimestamp, reportId)
                }
            reportDatabase.cellTowerDao().insertAll(*cellTowerEntities.toTypedArray())

            val bluetoothBeaconEntities =
                reportData.bluetoothBeacons.map {
                    BluetoothBeaconEntity.fromBluetoothBeacon(reportId, reportTimestamp, it)
                }
            reportDatabase.bluetoothBeaconDao().insertAll(*bluetoothBeaconEntities.toTypedArray())

            Timber.i(
                "Inserted report with %d Wi-Fi access points, %d cell towers and %d Bluetooth beacons to DB",
                wifiAccessPointEntities.size,
                cellTowerEntities.size,
                bluetoothBeaconEntities.size,
            )
        }
    }

    override suspend fun markAsUploaded(uploadTimestamp: Instant, vararg reportIds: Long) {
        reportDatabaseManager.reportDb.value.reportDao().markUpdated(uploadTimestamp, *reportIds)
    }
}
