package xyz.malkki.neostumbler.di

import android.content.Context
import org.koin.dsl.module
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication.Companion.EXPORT_NOTIFICATION_CHANNEL_ID
import xyz.malkki.neostumbler.export.CsvExportManager
import xyz.malkki.neostumbler.export.CsvExporter
import xyz.malkki.neostumbler.export.DatabaseExportManager
import xyz.malkki.neostumbler.export.DatabaseExporter
import xyz.malkki.neostumbler.export.WorkerCsvExportManager
import xyz.malkki.neostumbler.export.WorkerDatabaseExportManager

val exportModule = module {
    single {
        DatabaseExporter(
            tempFileDir = get<Context>().cacheDir.toPath(),
            rawReportImportExport = get(),
        )
    }

    single<DatabaseExportManager> {
        WorkerDatabaseExportManager(
            notificationChannelId = EXPORT_NOTIFICATION_CHANNEL_ID,
            notificationTitle = R.string.notification_exporting_data,
            notificationIconDrawable = R.drawable.upload_24px,
            context = get(),
        )
    }

    single { CsvExporter(get()) }

    single<CsvExportManager> {
        WorkerCsvExportManager(
            notificationChannelId = EXPORT_NOTIFICATION_CHANNEL_ID,
            notificationTitle = R.string.notification_exporting_data,
            notificationIconDrawable = R.drawable.upload_24px,
            context = get(),
        )
    }
}
