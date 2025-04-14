package xyz.malkki.neostumbler.scanner.passive

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.domain.CellTower.Companion.fillMissingData
import xyz.malkki.neostumbler.domain.LatLng
import xyz.malkki.neostumbler.domain.Position
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import xyz.malkki.neostumbler.scanner.ScanReportSaver
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
    context: Context,
    private val passiveScanStateManager: PassiveScanStateManager,
    private val scanReportSaver: ScanReportSaver,
    private val postProcessors: List<ReportPostProcessor>,
) {
    private val appContext = context.applicationContext

    private val wifiManager = appContext.getSystemService<WifiManager>()!!

    private val subscriptionManager = appContext.getSystemService<SubscriptionManager>()!!
    private val telephonyManager = appContext.getSystemService<TelephonyManager>()!!

    @RequiresPermission(
        allOf =
            [
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
            ]
    )
    suspend fun createPassiveScanReport(positions: List<Position>) {
        if (ScannerService.serviceRunning.value) {
            // If the active scanning service is running, we don't need to create passive reports
            return
        }

        val filteredPositions =
            positions.filter {
                it.accuracy != null && it.accuracy <= ScanningConstants.LOCATION_MAX_ACCURACY_METERS
            }

        if (filteredPositions.isEmpty()) {
            return
        }

        val lastLocation = passiveScanStateManager.getLastReportLocation()
        if (
            lastLocation != null &&
                filteredPositions.none {
                    LatLng(it.latitude, it.longitude).distanceTo(lastLocation) >
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
            ?.let { passiveScanStateManager.updateLastReportLocation(it.position.latLng) }

        reports.forEach { reportData -> scanReportSaver.saveReport(reportData) }
    }

    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE]
    )
    private suspend fun getWifiAccessPoints(): List<WifiAccessPoint> {
        val maxTimestamp = passiveScanStateManager.getMaxWifiTimestamp()

        return wifiManager.scanResults
            .map { WifiAccessPoint.fromScanResult(it) }
            .filter { maxTimestamp == null || it.timestamp > maxTimestamp }
    }

    @RequiresPermission(
        allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE]
    )
    private suspend fun getCellTowers(): List<CellTower> {
        val telephonyManagers =
            subscriptionManager.activeSubscriptionInfoList?.map {
                telephonyManager.createForSubscriptionId(it.subscriptionId)
            } ?: emptyList()

        val maxTimestamp = passiveScanStateManager.getMaxCellTimestamp()

        return telephonyManagers
            .flatMap {
                @SuppressLint("MissingPermission") val serviceState = it.serviceState

                it.allCellInfo
                    .mapNotNull { cellInfo -> CellTower.fromCellInfo(cellInfo) }
                    .fillMissingData(serviceState?.operatorNumeric)
                    .filter { it.hasEnoughData() }
            }
            .filter { maxTimestamp == null || it.timestamp > maxTimestamp }
    }
}
