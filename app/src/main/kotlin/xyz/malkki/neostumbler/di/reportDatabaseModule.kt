package xyz.malkki.neostumbler.di

import org.koin.dsl.module
import xyz.malkki.neostumbler.data.reports.RawReportImportExport
import xyz.malkki.neostumbler.data.reports.ReportExportProvider
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.reports.ReportRemover
import xyz.malkki.neostumbler.data.reports.ReportSaver
import xyz.malkki.neostumbler.data.reports.ReportStatisticsProvider
import xyz.malkki.neostumbler.data.reports.ReportStorageMetadataProvider
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.db.RoomRawReportImportExport
import xyz.malkki.neostumbler.db.RoomReportExportProvider
import xyz.malkki.neostumbler.db.RoomReportProvider
import xyz.malkki.neostumbler.db.RoomReportRemover
import xyz.malkki.neostumbler.db.RoomReportSaver
import xyz.malkki.neostumbler.db.RoomReportStatisticsProvider
import xyz.malkki.neostumbler.db.RoomReportStorageMetadataProvider

val reportDatabaseModule = module {
    single { ReportDatabaseManager(get()) }

    single<ReportStorageMetadataProvider> { RoomReportStorageMetadataProvider(get()) }

    single<RawReportImportExport> { RoomRawReportImportExport(get(), get()) }

    single<ReportStatisticsProvider> { RoomReportStatisticsProvider(get()) }

    single<ReportProvider> { RoomReportProvider(get()) }

    single<ReportSaver> { RoomReportSaver(get()) }

    single<ReportRemover> { RoomReportRemover(get()) }

    single<ReportExportProvider> { RoomReportExportProvider(get()) }
}
