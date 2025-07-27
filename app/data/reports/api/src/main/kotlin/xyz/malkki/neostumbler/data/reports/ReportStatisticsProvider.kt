package xyz.malkki.neostumbler.data.reports

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface ReportStatisticsProvider {
    fun getNewWifisPerDay(): Flow<Map<LocalDate, Long>>

    fun getNewCellsPerDay(): Flow<Map<LocalDate, Long>>

    fun getNewBluetoothsPerDay(): Flow<Map<LocalDate, Long>>
}
