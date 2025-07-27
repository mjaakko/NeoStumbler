package xyz.malkki.neostumbler.db

import android.content.Context
import java.nio.file.Path
import xyz.malkki.neostumbler.data.reports.RawReportImportExport
import xyz.malkki.neostumbler.db.extensions.copyTo

class RoomRawReportImportExport(
    private val context: Context,
    private val reportDatabaseManager: ReportDatabaseManager,
) : RawReportImportExport {
    override suspend fun validateRawReports(file: Path): Boolean {
        return ReportDatabaseManager.validateDatabase(context, file)
    }

    override suspend fun importRawReports(file: Path) {
        reportDatabaseManager.importDb(file)
    }

    override suspend fun exportRawReports(file: Path) {
        reportDatabaseManager.reportDb.value.openHelper.writableDatabase.copyTo(file)
    }
}
