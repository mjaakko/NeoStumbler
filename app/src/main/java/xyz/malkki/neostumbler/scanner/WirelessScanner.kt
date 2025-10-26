package xyz.malkki.neostumbler.scanner

import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.airpressure.AirPressureObservation
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.Emitter
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.core.report.ReportData
import xyz.malkki.neostumbler.extensions.combineWithLatestFrom
import xyz.malkki.neostumbler.scanner.movement.ConstantMovementDetector
import xyz.malkki.neostumbler.scanner.movement.MovementDetector
import xyz.malkki.neostumbler.scanner.postprocess.ReportPostProcessor

// Don't emit new locations until the distance between them is at least 30 metres or when at least
// 10 seconds have passed
private val LOCATION_MAX_AGE_UNTIL_CHANGED = 10.seconds
private const val LOCATION_MAX_DISTANCE_DIFF_UNTIL_CHANGED = 30

// Maximum age of air pressure data, relative to the location timestamp
private val AIR_PRESSURE_MAX_AGE = 2.seconds

// Retain locations in the last 30 seconds
private val LOCATION_BUFFER_DURATION = 30.seconds

class WirelessScanner(
    private val locationSource: () -> Flow<PositionObservation>,
    private val airPressureSource: () -> Flow<AirPressureObservation>,
    private val cellInfoSource: () -> Flow<List<EmitterObservation<CellTower, String>>>,
    private val wifiAccessPointSource:
        () -> Flow<List<EmitterObservation<WifiAccessPoint, MacAddress>>>,
    private val bluetoothBeaconSource:
        () -> Flow<List<EmitterObservation<BluetoothBeacon, MacAddress>>>,
    private val movementDetector: MovementDetector = ConstantMovementDetector,
    private val postProcessors: List<ReportPostProcessor> = emptyList(),
) {
    private fun getLocationsWithAirPressure(): Flow<PositionObservation> {
        val locationFlow = locationSource.invoke()
        val airPressureFlow = airPressureSource.invoke()

        return locationFlow.combineWithLatestFrom(airPressureFlow) { location, airPressure ->
            location.copy(
                position =
                    location.position.copy(
                        pressure =
                            airPressure
                                ?.takeIf {
                                    // Use air pressure data only if it's not too old
                                    abs(location.timestamp - it.timestamp).milliseconds <=
                                        AIR_PRESSURE_MAX_AGE
                                }
                                ?.airPressure
                                ?.toDouble()
                    )
            )
        }
    }

    private suspend fun <E : Emitter<K>, K> startCollectingData(
        isMovingFlow: Flow<Boolean>,
        dataSource: () -> Flow<List<EmitterObservation<E, K>>>,
        mapMutex: Mutex,
        map: MutableMap<K, EmitterObservation<E, K>>,
    ) {
        isMovingFlow
            .flatMapLatest { isMoving ->
                if (isMoving) {
                    dataSource.invoke()
                } else {
                    emptyFlow()
                }
            }
            .collect { data ->
                mapMutex.withLock { data.forEach { map[it.emitter.uniqueKey] = it } }
            }
    }

    private fun Flow<PositionObservation>.filterInaccurateLocations(): Flow<PositionObservation> =
        filter { positionObservation ->
            positionObservation.position.accuracy != null &&
                positionObservation.position.accuracy!! <=
                    ScanningConstants.LOCATION_MAX_ACCURACY_METERS
        }

    private fun Flow<PositionObservation>.distinctUntilChangedSignificantly():
        Flow<PositionObservation> = distinctUntilChanged { a, b ->
        abs(a.timestamp - b.timestamp).milliseconds <= LOCATION_MAX_AGE_UNTIL_CHANGED &&
            a.position.latLng.distanceTo(b.position.latLng) <=
                LOCATION_MAX_DISTANCE_DIFF_UNTIL_CHANGED
    }

    fun createReports(): Flow<ReportData> = channelFlow {
        val mutex = Mutex()

        val wifiAccessPointByMacAddr =
            mutableMapOf<MacAddress, EmitterObservation<WifiAccessPoint, MacAddress>>()
        val bluetoothBeaconsByMacAddr =
            mutableMapOf<MacAddress, EmitterObservation<BluetoothBeacon, MacAddress>>()
        val cellTowersByKey = mutableMapOf<String, EmitterObservation<CellTower, String>>()

        val isMovingFlow =
            movementDetector
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
            startCollectingData(
                isMovingFlow,
                wifiAccessPointSource,
                mutex,
                wifiAccessPointByMacAddr,
            )
        }

        launch(Dispatchers.Default) {
            startCollectingData(
                isMovingFlow,
                bluetoothBeaconSource,
                mutex,
                bluetoothBeaconsByMacAddr,
            )
        }

        launch(Dispatchers.Default) {
            startCollectingData(isMovingFlow, cellInfoSource, mutex, cellTowersByKey)
        }

        isMovingFlow
            .flatMapLatest { isMoving ->
                if (isMoving) {
                    getLocationsWithAirPressure()
                } else {
                    emptyFlow()
                }
            }
            .filterInaccurateLocations()
            .distinctUntilChangedSignificantly()
            // Collect locations to a list so that we can choose the best based on timestamp
            .bufferWithMaxAge(LOCATION_BUFFER_DURATION)
            .filter { it.isNotEmpty() }
            /*
             * Because network scans take a few seconds to complete, let's add a small delay to location updates
             * so that it's more likely that the data ends up in a single report
             */
            .onEach { delay(3.seconds) }
            .map { locations ->
                val (cells, wifis, bluetooths) =
                    mutex.withLock {
                        val cells = cellTowersByKey.values.toList()
                        cellTowersByKey.clear()

                        val wifis = wifiAccessPointByMacAddr.values.toList()
                        wifiAccessPointByMacAddr.clear()

                        val bluetooths = bluetoothBeaconsByMacAddr.values.toList()
                        bluetoothBeaconsByMacAddr.clear()

                        Triple(cells, wifis, bluetooths)
                    }

                createReports(locations, cells, wifis, bluetooths, postProcessors)
            }
            .collect { reports -> reports.forEach { report -> send(report) } }
    }

    private fun Flow<PositionObservation>.bufferWithMaxAge(
        maxAge: Duration
    ): Flow<List<PositionObservation>> {
        return scan(mutableListOf()) { list, observation ->
            list.add(observation)

            list.removeIf { abs(it.timestamp - observation.timestamp).milliseconds >= maxAge }

            list
        }
    }
}
