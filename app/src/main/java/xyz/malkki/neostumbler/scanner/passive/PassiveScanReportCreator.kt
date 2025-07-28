package xyz.malkki.neostumbler.scanner.passive

import android.Manifest
import androidx.annotation.RequiresPermission
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.data.emitter.PassiveCellTowerSource
import xyz.malkki.neostumbler.data.emitter.PassiveWifiAccessPointSource
import xyz.malkki.neostumbler.data.reports.ReportSaver
import xyz.malkki.neostumbler.geography.LatLng
import xyz.malkki.neostumbler.scanner.ScannerService
import xyz.malkki.neostumbler.scanner.ScanningConstants
import xyz.malkki.neostumbler.scanner.createReports
import xyz.malkki.neostumbler.scanner.postprocess.ReportPostProcessor

/**
 * Minimum distance from the location where the last passive report was made. This is used to avoid
 * creating a lot of duplicate reports when the user is not moving
 */
private const val MIN_DISTANCE_FROM_LAST_LOCATION = 50

class PassiveScanReportCreator(
    private val passiveWifiAccessPointSource: PassiveWifiAccessPointSource,
    private val passiveCellTowerSource: PassiveCellTowerSource,
    private val passiveScanStateManager: PassiveScanStateManager,
    private val reportSaver: ReportSaver,
    private val postProcessors: List<ReportPostProcessor>,
) {
    @RequiresPermission(
        allOf =
            [
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
            ]
    )
    suspend fun createPassiveScanReport(positions: List<PositionObservation>) {
        if (ScannerService.serviceRunning.value) {
            // If the active scanning service is running, we don't need to create passive reports
            return
        }

        val filteredPositions =
            positions.filter {
                it.position.accuracy != null &&
                    it.position.accuracy!! <= ScanningConstants.LOCATION_MAX_ACCURACY_METERS
            }

        if (filteredPositions.isEmpty()) {
            return
        }

        val lastLocation = passiveScanStateManager.getLastReportLocation()
        if (
            lastLocation != null &&
                filteredPositions.none {
                    LatLng(it.position.latitude, it.position.longitude).distanceTo(lastLocation) >
                        MIN_DISTANCE_FROM_LAST_LOCATION
                }
        ) {
            return
        }

        val cellTowers = getCellTowers()
        cellTowers
            .maxOfOrNull { it.timestamp }
            ?.let { passiveScanStateManager.updateMaxCellTimestamp(it) }

        val wifiAccessPoints = getWifiAccessPoints()
        wifiAccessPoints
            .maxOfOrNull { it.timestamp }
            ?.let { passiveScanStateManager.updateMaxWifiTimestamp(it) }

        val reports =
            createReports(
                positions = filteredPositions,
                cellTowers = cellTowers,
                wifiAccessPoints = wifiAccessPoints,
                // Currently we don't support passive scanning for Bluetooth beacons
                bluetoothBeacons = emptyList(),
                postProcessors = postProcessors,
            )

        reports
            .maxByOrNull { it.position.timestamp }
            ?.let { passiveScanStateManager.updateLastReportLocation(it.position.position.latLng) }

        reports.forEach { reportData -> reportSaver.createReport(reportData) }
    }

    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE]
    )
    private suspend fun getWifiAccessPoints():
        List<EmitterObservation<WifiAccessPoint, MacAddress>> {
        val maxTimestamp = passiveScanStateManager.getMaxWifiTimestamp()

        return passiveWifiAccessPointSource.getWifiAccessPoints().filter {
            maxTimestamp == null || it.timestamp > maxTimestamp
        }
    }

    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE]
    )
    private suspend fun getCellTowers(): List<EmitterObservation<CellTower, String>> {
        val maxTimestamp = passiveScanStateManager.getMaxCellTimestamp()

        return passiveCellTowerSource.getCellTowers().filter {
            maxTimestamp == null || it.timestamp > maxTimestamp
        }
    }
}
