package xyz.malkki.neostumbler.db

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.core.report.Report
import xyz.malkki.neostumbler.core.report.ReportEmitter
import xyz.malkki.neostumbler.core.report.ReportWithLocation
import xyz.malkki.neostumbler.core.report.ReportWithStats
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.db.dao.getReportsInsideBoundingBox
import xyz.malkki.neostumbler.db.entities.ReportWithData
import xyz.malkki.neostumbler.db.entities.toBluetoothBeacon
import xyz.malkki.neostumbler.db.entities.toCellTower
import xyz.malkki.neostumbler.db.entities.toReportPosition
import xyz.malkki.neostumbler.db.entities.toWifiAccessPoint

private const val PAGE_SIZE = 25

class RoomReportProvider(private val reportDatabaseManager: ReportDatabaseManager) :
    ReportProvider {
    override fun getReportCount(): Flow<Int> {
        return reportDatabaseManager.reportDb.flatMapLatest { reportDatabase ->
            reportDatabase.reportDao().getReportCount()
        }
    }

    override fun getNotUploadedReportCount(): Flow<Int> {
        return reportDatabaseManager.reportDb.flatMapLatest { reportDatabase ->
            reportDatabase.reportDao().getReportCountNotUploaded()
        }
    }

    override fun getLastReportUploadTime(): Flow<Instant?> {
        return reportDatabaseManager.reportDb.flatMapLatest { reportDatabase ->
            reportDatabase.reportDao().getLastUploadTime()
        }
    }

    override fun getReportDates(): Flow<Set<LocalDate>> {
        return reportDatabaseManager.reportDb
            .flatMapLatest { reportDatabase -> reportDatabase.reportDao().getReportDates() }
            .map { it.toSet() }
    }

    override fun getReportsWithStats(): Flow<PagingData<ReportWithStats>> {
        return reportDatabaseManager.reportDb.flatMapLatest { reportDatabase ->
            Pager(PagingConfig(pageSize = PAGE_SIZE)) {
                    reportDatabase.reportDao().getAllReportsWithStats()
                }
                .flow
        }
    }

    override fun getReport(reportId: Long): Flow<Report> {
        return reportDatabaseManager.reportDb
            .flatMapLatest { reportDatabase -> reportDatabase.reportDao().getReport(reportId) }
            .map { reportEntity -> reportEntity.toReport() }
    }

    override fun getLatestReportLocation(): Flow<ReportWithLocation?> {
        return reportDatabaseManager.reportDb.flatMapLatest { reportDatabase ->
            reportDatabase.reportDao().getLatestReport()
        }
    }

    override suspend fun getNotUploadedReports(count: Int): List<Report> {
        return reportDatabaseManager.reportDb.value.reportDao().getNotUploadedReports(count).map {
            it.toReport()
        }
    }

    override suspend fun getRandomNotUploadedReports(count: Int): List<Report> {
        return reportDatabaseManager.reportDb.value
            .reportDao()
            .getRandomNotUploadedReports(count)
            .map { it.toReport() }
    }

    override suspend fun getReportsForTimerange(
        fromTimestamp: Instant,
        toTimestamp: Instant,
    ): List<Report> {
        return reportDatabaseManager.reportDb.value
            .reportDao()
            .getAllReportsForTimerange(fromTimestamp, toTimestamp)
            .map { it.toReport() }
    }

    override suspend fun getReportsNewerThan(timestamp: Instant): List<ReportWithLocation> {
        return reportDatabaseManager.reportDb.value.reportDao().getReportsNewerThan(timestamp)
    }

    override fun getReportsInsideBoundingBox(
        minLatitude: Double,
        minLongitude: Double,
        maxLatitude: Double,
        maxLongitude: Double,
    ): Flow<List<ReportWithLocation>> {
        return reportDatabaseManager.reportDb.flatMapLatest { reportDatabase ->
            reportDatabase
                .reportDao()
                .getReportsInsideBoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude)
        }
    }
}

private fun ReportWithData.toReport(): Report {
    return Report(
        id = report.id,
        timestamp = report.timestamp,
        uploaded = report.uploaded,
        uploadTimestamp = report.uploadTimestamp,
        position = positionEntity.toReportPosition(),
        wifiAccessPoints =
            wifiAccessPointEntities.map {
                ReportEmitter(id = it.id!!, emitter = it.toWifiAccessPoint(), age = it.age)
            },
        cellTowers =
            cellTowerEntities.map {
                ReportEmitter(id = it.id, emitter = it.toCellTower(), age = it.age)
            },
        bluetoothBeacons =
            bluetoothBeaconEntities.map {
                ReportEmitter(id = it.id, emitter = it.toBluetoothBeacon(), age = it.age)
            },
    )
}
