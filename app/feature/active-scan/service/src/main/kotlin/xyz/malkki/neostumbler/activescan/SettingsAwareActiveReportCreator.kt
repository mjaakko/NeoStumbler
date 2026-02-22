package xyz.malkki.neostumbler.activescan

import kotlinx.coroutines.flow.flatMapLatest
import xyz.malkki.neostumbler.data.reports.ReportSaver
import xyz.malkki.neostumbler.data.settings.Settings

/**
 * Report creator, which actively scans for wireless devices, restarting scanning whenever settings
 * change
 */
class SettingsAwareActiveReportCreator(
    private val settings: Settings,
    private val activeScanner: ActiveScanner,
    private val reportSaver: ReportSaver,
) {
    suspend fun scanAndCreateReports(onReportCreated: () -> Unit, onGpsActive: (Boolean) -> Unit) {
        settings
            .getSnapshotFlow()
            .flatMapLatest { settingsSnapshot ->
                activeScanner.getReportsFlow(settingsSnapshot.toActiveScanSettings(), onGpsActive)
            }
            .collect { reportData ->
                reportSaver.createReport(reportData)

                onReportCreated()
            }
    }
}
