package xyz.malkki.neostumbler.scanner

import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import xyz.malkki.neostumbler.domain.BluetoothBeacon
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.domain.ObservedDevice
import xyz.malkki.neostumbler.domain.Position
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import xyz.malkki.neostumbler.scanner.data.ReportData
import xyz.malkki.neostumbler.scanner.postprocess.ReportPostProcessor

// Maximum age for observed devices. This is used to filter out old data when e.g. there is no GPS
// signal and there's a gap between two locations
private val OBSERVED_DEVICE_MAX_AGE = 30.seconds

fun createReports(
    positions: List<Position>,
    cellTowers: List<CellTower>,
    wifiAccessPoints: List<WifiAccessPoint>,
    bluetoothBeacons: List<BluetoothBeacon>,
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

private fun <D : ObservedDevice<*>> List<D>.groupByMinTimestampDiff(
    positions: List<Position>
): Map<Position, List<D>> = groupBy { device ->
    positions.minBy { position -> abs(position.timestamp - device.timestamp) }
}

private fun <D : ObservedDevice<*>> Map<Position, List<D>>.filterOldData() =
    mapValues { (position, devices) ->
            devices.filter { device ->
                abs(device.timestamp - position.timestamp).milliseconds <= OBSERVED_DEVICE_MAX_AGE
            }
        }
        .filterValues { it.isNotEmpty() }

private fun ReportData.postProcess(postProcessors: List<ReportPostProcessor>): ReportData? {
    return postProcessors.fold<ReportPostProcessor, ReportData?>(this) { reportToProcess, processor
        ->
        reportToProcess?.let { processor.postProcessReport(it) }
    }
}
