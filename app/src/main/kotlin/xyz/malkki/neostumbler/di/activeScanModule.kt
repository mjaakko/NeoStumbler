package xyz.malkki.neostumbler.di

import android.content.Context
import android.widget.Toast
import org.koin.dsl.module
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.activescan.ActiveScanManager
import xyz.malkki.neostumbler.activescan.ActiveScanSettingsManager
import xyz.malkki.neostumbler.activescan.ActiveScanner
import xyz.malkki.neostumbler.activescan.AndroidActiveScanManager
import xyz.malkki.neostumbler.activescan.SettingsAwareActiveReportCreator
import xyz.malkki.neostumbler.activescan.adapter.ScanNotificationAdapter
import xyz.malkki.neostumbler.activescan.adapter.ScanStartErrorHandler
import xyz.malkki.neostumbler.activescan.adapter.ScannerQSTileAdapter
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.report.postprocessor.ReportPostProcessor
import xyz.malkki.neostumbler.scanner.notify.ScanNotificationCreator
import xyz.malkki.neostumbler.scanner.quicksettings.ScannerQSTileUpdater

val activeScanModule = module {
    single<ScanNotificationAdapter> { ScanNotificationCreator() }

    single<ActiveScanManager> { AndroidActiveScanManager(get()) }

    factory<ActiveScanner> {
        ActiveScanner(
            locationSourceProvider = get(),
            airPressureSource = get(),
            cellInfoSource = get(),
            wifiAccessPointSource = get(),
            bluetoothBeaconSource = get(),
            movementDetectorProvider = get(),
            batteryLevelMonitor = get(),
            thermalStatusProvider = get(),
            postProcessors = getAll<ReportPostProcessor>(),
        )
    }

    single<SettingsAwareActiveReportCreator> {
        SettingsAwareActiveReportCreator(get(), get(), get())
    }

    single<ActiveScanSettingsManager> { ActiveScanSettingsManager(get()) }

    single<ScannerQSTileAdapter> { ScannerQSTileUpdater(get()) }

    single<ScanStartErrorHandler> {
        val context = get<Context>()

        ScanStartErrorHandler {
            context.showToast(
                context.getString(R.string.scanning_start_error_not_in_foreground),
                Toast.LENGTH_LONG,
            )
        }
    }
}
