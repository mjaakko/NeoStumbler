package xyz.malkki.neostumbler.db

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import xyz.malkki.neostumbler.data.reports.ReportStatisticsProvider

class RoomReportStatisticsProvider(private val reportDatabaseManager: ReportDatabaseManager) :
    ReportStatisticsProvider {
    override fun getNewWifisPerDay(): Flow<Map<LocalDate, Long>> {
        return reportDatabaseManager.reportDb.flatMapLatest { db ->
            db.statisticsDao().newWifisPerDay()
        }
    }

    override fun getNewCellsPerDay(): Flow<Map<LocalDate, Long>> {
        return reportDatabaseManager.reportDb.flatMapLatest { db ->
            db.statisticsDao().newCellsPerDay()
        }
    }

    override fun getNewBluetoothsPerDay(): Flow<Map<LocalDate, Long>> {
        return reportDatabaseManager.reportDb.flatMapLatest { db ->
            db.statisticsDao().newBeaconsPerDay()
        }
    }
}
