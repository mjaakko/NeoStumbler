package xyz.malkki.neostumbler.scanner

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import xyz.malkki.neostumbler.common.LocationWithSource
import xyz.malkki.neostumbler.domain.AirPressureObservation
import xyz.malkki.neostumbler.domain.BluetoothBeacon
import xyz.malkki.neostumbler.domain.CellTower
import xyz.malkki.neostumbler.domain.ObservedDevice
import xyz.malkki.neostumbler.domain.Position
import xyz.malkki.neostumbler.domain.WifiAccessPoint
import xyz.malkki.neostumbler.extensions.buffer
import xyz.malkki.neostumbler.extensions.combineWithLatestFrom
import xyz.malkki.neostumbler.extensions.elapsedRealtimeMillisCompat
import xyz.malkki.neostumbler.scanner.data.ReportData
import xyz.malkki.neostumbler.scanner.movement.ConstantMovementDetector
import xyz.malkki.neostumbler.scanner.movement.MovementDetector
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

//Maximum accuracy for locations, used for filtering bad locations
private const val LOCATION_MAX_ACCURACY = 200

//Maximum age for locations
private val LOCATION_MAX_AGE = 20.seconds

//Maximum age for observed devices. This is used to filter out old data when e.g. there is no GPS signal and there's a gap between two locations
private val OBSERVED_DEVICE_MAX_AGE = 30.seconds

//Maximum age of air pressure data, relative to the location timestamp
private val AIR_PRESSURE_MAX_AGE = 2.seconds

//Retain locations in the last 10 seconds
private val LOCATION_BUFFER_DURATION = 10.seconds

/**
 * @param timeSource Time source used in the data, defaults to [SystemClock.elapsedRealtime]
 */
class WirelessScanner(
    private val locationSource: () -> Flow<LocationWithSource>,
    private val airPressureSource: () -> Flow<AirPressureObservation>,
    private val cellInfoSource: () -> Flow<List<CellTower>>,
    private val wifiAccessPointSource: () -> Flow<List<WifiAccessPoint>>,
    private val bluetoothBeaconSource: () -> Flow<List<BluetoothBeacon>>,
    private val movementDetector: MovementDetector = ConstantMovementDetector,
    private val timeSource: () -> Long = SystemClock::elapsedRealtime
) {
    fun createReports(): Flow<ReportData> = channelFlow {
        val mutex = Mutex()

        val wifiAccessPointByMacAddr = mutableMapOf<String, WifiAccessPoint>()
        val bluetoothBeaconsByMacAddr = mutableMapOf<String, BluetoothBeacon>()
        val cellTowersByKey = mutableMapOf<String, CellTower>()

        val isMovingFlow = movementDetector
            .getIsMovingFlow()
            .onEach {
                if (it) {
                    Timber.i("Moving started, resuming scanning")
                } else {
                    Timber.i("Moving stopped, pausing scanning")
                }
            }
            .stateIn(this)

        launch(Dispatchers.Default) {
            isMovingFlow
                .flatMapLatest { isMoving ->
                    if (isMoving) {
                        wifiAccessPointSource.invoke()
                    } else {
                        emptyFlow()
                    }
                }
                .map {
                    it.filterHiddenNetworks()
                }
                .collect { wifiAccessPoints ->
                    mutex.withLock {
                        wifiAccessPoints.forEach { wifiAccessPoint ->
                            wifiAccessPointByMacAddr[wifiAccessPoint.macAddress] = wifiAccessPoint
                        }
                    }
                }
        }

        launch(Dispatchers.Default) {
            isMovingFlow
                .flatMapLatest { isMoving ->
                    if (isMoving) {
                        bluetoothBeaconSource.invoke()
                    } else {
                        emptyFlow()
                    }
                }
                .collect { bluetoothBeacons ->
                    mutex.withLock {
                        bluetoothBeacons.forEach { bluetoothBeacon ->
                            bluetoothBeaconsByMacAddr[bluetoothBeacon.macAddress] = bluetoothBeacon
                        }
                    }
                }
        }

        launch(Dispatchers.Default) {
            isMovingFlow
                .flatMapLatest { isMoving ->
                    if (isMoving) {
                        cellInfoSource.invoke()
                    } else {
                        emptyFlow()
                    }
                }
                .collect { cellTowers ->
                    mutex.withLock {
                        cellTowers.forEach { cellTower ->
                            cellTowersByKey[cellTower.key] = cellTower
                        }
                    }
                }
        }

        isMovingFlow
            .flatMapLatest { isMoving ->
                if (isMoving) {
                    val locationFlow = locationSource.invoke()
                    val airPressureFlow = airPressureSource.invoke()

                    locationFlow.combineWithLatestFrom(airPressureFlow) { location, airPressure ->
                        //Use air pressure data only if it's not too old
                        location to airPressure?.takeIf {
                            abs(location.location.elapsedRealtimeMillisCompat - it.timestamp).milliseconds <= AIR_PRESSURE_MAX_AGE
                        }
                    }
                } else {
                    emptyFlow()
                }
            }
            .filter { (location, _) ->
                location.location.hasAccuracy() && location.location.accuracy <= LOCATION_MAX_ACCURACY
            }
            .filter { (location, _) ->
                val age = (timeSource.invoke() - location.location.elapsedRealtimeMillisCompat).milliseconds

                age <= LOCATION_MAX_AGE
            }
            //Collect locations to a list so that we can choose the best based on timestamp
            .buffer(LOCATION_BUFFER_DURATION)
            .filter {
                it.isNotEmpty()
            }
            .map { locations ->
                val (cells, wifis, bluetooths) = mutex.withLock {
                    val cells = cellTowersByKey.values.toList()
                    cellTowersByKey.clear()

                    //Take Wi-Fis only if there's at least two, because two are needed for a valid Geosubmit report
                    val wifis = if (wifiAccessPointByMacAddr.size >= 2) {
                        wifiAccessPointByMacAddr.values.toList().apply {
                            wifiAccessPointByMacAddr.clear()
                        }
                    } else {
                        emptyList()
                    }

                    val bluetooths = bluetoothBeaconsByMacAddr.values.toList()
                    bluetoothBeaconsByMacAddr.clear()

                    Triple(cells, wifis, bluetooths)
                }

                val now = timeSource.invoke()

                val cellsByLocation = cells
                    .filterOldData(now)
                    .groupByLocation(locations)

                val wifisByLocation = wifis
                    .filterOldData(now)
                    .groupByLocation(locations)

                val bluetoothsByLocation = bluetooths
                    .filterOldData(now)
                    .groupByLocation(locations)

                val locationsWithData = cellsByLocation.keys + wifisByLocation.keys + bluetoothsByLocation.keys

                locationsWithData
                    .map { location ->
                        createReport(location, cellsByLocation[location] ?: emptyList(), wifisByLocation[location] ?: emptyList(), bluetoothsByLocation[location] ?: emptyList())
                    }
                    .filter {
                        it.bluetoothBeacons.isNotEmpty() || it.cellTowers.isNotEmpty() || it.wifiAccessPoints.isNotEmpty()
                    }
            }
            .collect { reports ->
                reports.forEach {
                    send(it)
                }
            }
    }

    private val CellTower.key: String
        get() = listOf(mobileCountryCode, mobileNetworkCode, locationAreaCode, cellId, primaryScramblingCode).joinToString("/")

    private fun createReport(location: LocationWithAirPressure, cells: List<CellTower>, wifis: List<WifiAccessPoint>, bluetooths: List<BluetoothBeacon>): ReportData {
        return ReportData(
            position = Position.fromLocation(
                location = location.first.location,
                source = location.first.source.name.lowercase(Locale.ROOT),
                airPressure = location.second?.airPressure?.toDouble()
            ),
            cellTowers = cells,
            wifiAccessPoints = wifis.takeIf { it.size >= 2 } ?: emptyList(),
            bluetoothBeacons = bluetooths
        )
    }

    /**
     * Filters Wi-Fi networks that should not be sent to geolocation services, i.e.
     * hidden networks with empty SSID or those with SSID ending in "_nomap"
     *
     * @return Filtered list of scan results
     */
    private fun List<WifiAccessPoint>.filterHiddenNetworks(): List<WifiAccessPoint> = filter { wifiAccessPoint ->
        val ssid = wifiAccessPoint.ssid

        !ssid.isNullOrBlank() && !ssid.endsWith("_nomap")
    }

    private fun <T : ObservedDevice> List<T>.filterOldData(currentTimestamp: Long): List<T> = filter { device ->
        (currentTimestamp - device.timestamp).milliseconds <= OBSERVED_DEVICE_MAX_AGE
    }

    private fun <T : ObservedDevice> List<T>.groupByLocation(locations: List<LocationWithAirPressure>): Map<LocationWithAirPressure, List<T>> = groupBy {
        locations.minBy { location -> abs(it.timestamp - location.first.location.elapsedRealtimeMillisCompat) }
    }
}

private typealias LocationWithAirPressure = Pair<LocationWithSource, AirPressureObservation?>