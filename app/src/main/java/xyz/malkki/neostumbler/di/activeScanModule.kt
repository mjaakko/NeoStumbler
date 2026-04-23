package xyz.malkki.neostumbler.di

import org.koin.dsl.module
import xyz.malkki.neostumbler.activescan.ActiveScanManager
import xyz.malkki.neostumbler.activescan.ActiveScanSettingsManager
import xyz.malkki.neostumbler.activescan.ActiveScanner
import xyz.malkki.neostumbler.activescan.AndroidActiveScanManager
import xyz.malkki.neostumbler.activescan.SettingsAwareActiveReportCreator
import xyz.malkki.neostumbler.activescan.adapter.ScanNotificationAdapter
import xyz.malkki.neostumbler.activescan.adapter.ScannerQSTileAdapter
import xyz.malkki.neostumbler.scanner.notify.ScanNotificationCreator
import xyz.malkki.neostumbler.scanner.quicksettings.ScannerQSTileUpdater

val activeScanModule = module {
    single<ScanNotificationAdapter> { ScanNotificationCreator() }

    single<ActiveScanManager> { AndroidActiveScanManager(get()) }

    single<ActiveScanner> { ActiveScanner(get(), get(), get(), get(), get(), get(), get(), get()) }

    single<SettingsAwareActiveReportCreator> {
        SettingsAwareActiveReportCreator(get(), get(), get())
    }

    single<ActiveScanSettingsManager> { ActiveScanSettingsManager(get()) }

    single<ScannerQSTileAdapter> { ScannerQSTileUpdater(get()) }
}
