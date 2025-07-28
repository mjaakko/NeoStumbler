package xyz.malkki.neostumbler.scanner

import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.Emitter
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.core.report.ReportData
import xyz.malkki.neostumbler.scanner.postprocess.ReportPostProcessor

// Maximum age for observed devices. This is used to filter out old data when e.g. there is no GPS
// signal and there's a gap between two locations
private val OBSERVED_DEVICE_MAX_AGE = 30.seconds

fun createReports(
    positions: List<PositionObservation>,
    cellTowers: List<EmitterObservation<CellTower, String>>,
    wifiAccessPoints: List<EmitterObservation<WifiAccessPoint, MacAddress>>,
    bluetoothBeacons: List<EmitterObservation<BluetoothBeacon, MacAddress>>,
    postProcessors: List<ReportPostProcessor>,
): List<ReportData> {
    val cellTowersByPosition = cellTowers.groupByMinTimestampDiff(positions).filterOldData()

    val wifiAccessPointsByPosition =
        wifiAccessPoints.groupByMinTimestampDiff(positions).filterOldData()

    val bluetoothBeaconsByPosition =
        bluetoothBeacons.groupByMinTimestampDiff(positions).filterOldData()

    return positions
        .map { position ->
            val cellTowersForPosition = cellTowersByPosition[position] ?: emptyList()
            // At least 2 Wi-Fi access points are needed for valid Geosubmit reports
            val wifiAccessPointsForPosition =
                wifiAccessPointsByPosition[position]?.takeIf { it.size >= 2 } ?: emptyList()
            val bluetoothBeaconsForPosition = bluetoothBeaconsByPosition[position] ?: emptyList()

            ReportData(
                position = position,
                cellTowers = cellTowersForPosition,
                wifiAccessPoints = wifiAccessPointsForPosition,
                bluetoothBeacons = bluetoothBeaconsForPosition,
            )
        }
        .mapNotNull { report -> report.postProcess(postProcessors) }
        .filter { report -> !report.isEmpty }
}

private fun <E : Emitter<K>, K> List<EmitterObservation<E, K>>.groupByMinTimestampDiff(
    positions: List<PositionObservation>
): Map<PositionObservation, List<EmitterObservation<E, K>>> = groupBy { observation ->
    positions.minBy { position -> abs(position.timestamp - observation.timestamp) }
}

private fun <E : Emitter<K>, K> Map<PositionObservation, List<EmitterObservation<E, K>>>
    .filterOldData() =
    mapValues { (position, devices) ->
            devices.filter { observation ->
                abs(observation.timestamp - position.timestamp).milliseconds <=
                    OBSERVED_DEVICE_MAX_AGE
            }
        }
        .filterValues { it.isNotEmpty() }

private fun ReportData.postProcess(postProcessors: List<ReportPostProcessor>): ReportData? {
    return postProcessors.fold<ReportPostProcessor, ReportData?>(this) { reportToProcess, processor
        ->
        reportToProcess?.let { processor.postProcessReport(it) }
    }
}
