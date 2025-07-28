package xyz.malkki.neostumbler.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.data.reports.ReportStorageMetadataProvider
import xyz.malkki.neostumbler.db.extensions.getEstimatedSize
import xyz.malkki.neostumbler.db.extensions.getTableNames

class RoomReportStorageMetadataProvider(private val reportDatabaseManager: ReportDatabaseManager) :
    ReportStorageMetadataProvider {
    override fun getEstimatedSize(): Flow<Long> {
        return reportDatabaseManager.reportDb.flatMapLatest { database ->
            val tableNames = database.openHelper.readableDatabase.getTableNames()

            database.invalidationTracker
                .createFlow(*tableNames.toTypedArray(), emitInitialState = true)
                .map { database.openHelper.readableDatabase.getEstimatedSize() }
        }
    }

    override fun getSchemaVersion(): Int {
        return REPORT_DB_VERSION
    }
}
