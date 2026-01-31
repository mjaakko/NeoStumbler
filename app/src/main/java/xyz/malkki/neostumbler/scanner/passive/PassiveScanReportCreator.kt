package xyz.malkki.neostumbler.scanner.passive

import android.Manifest
import androidx.annotation.RequiresPermission
import xyz.malkki.neostumbler.core.emitter.Emitter
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.core.report.ReportData
import xyz.malkki.neostumbler.data.emitter.PassiveBluetoothBeaconSource
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
    private val passiveBluetoothBeaconSource: PassiveBluetoothBeaconSource,
    private val passiveScanStateManager: PassiveScanStateManager,
    private val reportSaver: ReportSaver,
    private val postProcessors: List<ReportPostProcessor>,
    private val activeScanningRunning: () -> Boolean = { ScannerService.serviceRunning.value },
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
        if (activeScanningRunning()) {
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

        val cellTowers =
            getObservations(
                    dataType = PassiveScanStateManager.DataType.CELL,
                    passiveCellTowerSource::getCellTowers,
                )
                .filterDuplicates()
        val wifiAccessPoints =
            getObservations(
                    dataType = PassiveScanStateManager.DataType.WIFI,
                    passiveWifiAccessPointSource::getWifiAccessPoints,
                )
                .filterDuplicates()
        val bluetoothBeacons =
            getObservations(
                    dataType = PassiveScanStateManager.DataType.BLUETOOTH,
                    passiveBluetoothBeaconSource::getBluetoothBeacons,
                )
                .filterDuplicates()

        val lastLocations =
            getLastLocations(
                buildList {
                    if (cellTowers.isNotEmpty()) {
                        add(PassiveScanStateManager.DataType.CELL)
                    }

                    if (wifiAccessPoints.isNotEmpty()) {
                        add(PassiveScanStateManager.DataType.WIFI)
                    }

                    if (bluetoothBeacons.isNotEmpty()) {
                        add(PassiveScanStateManager.DataType.BLUETOOTH)
                    }
                }
            )

        if (
            lastLocations.isNotEmpty() &&
                lastLocations.none { lastLocation ->
                    filteredPositions.any { filterPosition ->
                        filterPosition.position.latLng.distanceTo(lastLocation) >
                            MIN_DISTANCE_FROM_LAST_LOCATION
                    }
                }
        ) {
            return
        }

        cellTowers.updateMaxObservedTimestamp(dataType = PassiveScanStateManager.DataType.CELL)

        wifiAccessPoints.updateMaxObservedTimestamp(
            dataType = PassiveScanStateManager.DataType.WIFI
        )

        bluetoothBeacons.updateMaxObservedTimestamp(
            dataType = PassiveScanStateManager.DataType.BLUETOOTH
        )

        val reports =
            createReports(
                positions = filteredPositions,
                cellTowers = cellTowers,
                wifiAccessPoints = wifiAccessPoints,
                bluetoothBeacons = bluetoothBeacons,
                postProcessors = postProcessors,
            )

        reports.updateLastUsedPosition(PassiveScanStateManager.DataType.CELL) { it.cellTowers }

        reports.updateLastUsedPosition(PassiveScanStateManager.DataType.WIFI) {
            it.wifiAccessPoints
        }

        reports.updateLastUsedPosition(PassiveScanStateManager.DataType.BLUETOOTH) {
            it.bluetoothBeacons
        }

        reports.forEach { reportData -> reportSaver.createReport(reportData) }
    }

    private suspend fun getLastLocations(
        dataTypes: Collection<PassiveScanStateManager.DataType>
    ): List<LatLng> {
        return dataTypes.mapNotNull { dataType ->
            passiveScanStateManager.getLastReportLocation(dataType)
        }
    }

    /** Filters duplicate observations by choosing the one with most recent timestamp */
    private fun <T : Emitter<K>, K> List<EmitterObservation<T, K>>.filterDuplicates():
        List<EmitterObservation<T, K>> {
        return groupBy { it.emitter.uniqueKey }
            .mapValues { observations ->
                observations.value.maxByOrNull { cellTower -> cellTower.timestamp }
            }
            .values
            .filterNotNull()
    }

    private suspend fun <E : Emitter<K>, K> getObservations(
        dataType: PassiveScanStateManager.DataType,
        source: suspend () -> List<EmitterObservation<E, K>>,
    ): List<EmitterObservation<E, K>> {
        val maxTimestamp = passiveScanStateManager.getMaxTimestamp(dataType = dataType)

        return source().filter { maxTimestamp == null || it.timestamp > maxTimestamp }
    }

    private suspend fun List<EmitterObservation<*, *>>.updateMaxObservedTimestamp(
        dataType: PassiveScanStateManager.DataType
    ) {
        maxOfOrNull { it.timestamp }
            ?.let {
                passiveScanStateManager.updateMaxTimestamp(dataType = dataType, timestamp = it)
            }
    }

    private suspend fun List<ReportData>.updateLastUsedPosition(
        dataType: PassiveScanStateManager.DataType,
        extractData: (ReportData) -> List<EmitterObservation<*, *>>,
    ) {
        filter { extractData(it).isNotEmpty() }
            .maxByOrNull { it.position.timestamp }
            ?.let {
                passiveScanStateManager.updateLastReportLocation(
                    dataType = dataType,
                    latLng = it.position.position.latLng,
                )
            }
    }
}
