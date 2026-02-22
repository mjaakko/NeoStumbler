package xyz.malkki.neostumbler.activescan

import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.malkki.neostumbler.activescan.internal.combineWithLatestFrom
import xyz.malkki.neostumbler.activescan.internal.toSmoothenedSpeedFlow
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.Emitter
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.core.report.ReportData
import xyz.malkki.neostumbler.data.airpressure.AirPressureSource
import xyz.malkki.neostumbler.data.battery.BatteryLevelMonitor
import xyz.malkki.neostumbler.data.emitter.ActiveBluetoothBeaconSource
import xyz.malkki.neostumbler.data.emitter.ActiveCellInfoSource
import xyz.malkki.neostumbler.data.emitter.ActiveWifiAccessPointSource
import xyz.malkki.neostumbler.data.location.LocationSourceProvider
import xyz.malkki.neostumbler.data.movement.MovementDetectorProvider
import xyz.malkki.neostumbler.report.postprocessor.ReportPostProcessorProvider
import xyz.malkki.neostumbler.scanner.ScanningConstants

/**
 * Try to get new locations every 3 seconds
 *
 * If the interval is too high, data quality will be worse because of the distance traveled between
 * the scan and the location fix. Also there can be some gaps on the map
 *
 * If the interval is too low, there will be many reports with just a few observations which will
 * decrease DB performance in the long term and the reports UI will be cluttered. Also there seems
 * to be a noticeable effect on battery life
 *
 * 3 seconds seems to give a reasonably good balance between these
 */
private val LOCATION_INTERVAL = 3.seconds

// Don't emit new locations until the distance between them is at least 30 metres or when at least
// 10 seconds have passed
private val LOCATION_MAX_AGE_UNTIL_CHANGED = 10.seconds
private const val LOCATION_MAX_DISTANCE_DIFF_UNTIL_CHANGED = 30

// Maximum age of air pressure data, relative to the location timestamp
private val AIR_PRESSURE_MAX_AGE = 2.seconds

// Retain locations in the last 30 seconds
private val LOCATION_BUFFER_DURATION = 30.seconds

class ActiveScanner(
    private val locationSourceProvider: LocationSourceProvider,
    private val airPressureSource: AirPressureSource,
    private val cellInfoSource: ActiveCellInfoSource,
    private val wifiAccessPointSource: ActiveWifiAccessPointSource,
    private val bluetoothBeaconSource: ActiveBluetoothBeaconSource,
    private val movementDetectorProvider: MovementDetectorProvider,
    private val batteryLevelMonitor: BatteryLevelMonitor,
    private val postProcessorProvider: ReportPostProcessorProvider,
) {
    private fun Flow<PositionObservation>.combineLocationsWithAirPressure():
        Flow<PositionObservation> {
        return combineWithLatestFrom(airPressureSource.getAirPressureFlow(LOCATION_INTERVAL / 2)) {
            location,
            airPressure ->
            airPressure
                ?.takeIf {
                    // Use air pressure data only if it's not too old
                    abs(location.timestamp - it.timestamp).milliseconds <= AIR_PRESSURE_MAX_AGE
                }
                ?.let { pressure ->
                    location.copy(
                        position =
                            location.position.copy(pressure = pressure.airPressure.toDouble())
                    )
                } ?: location
        }
    }

    private fun <E : Emitter<K>, K> CoroutineScope.startCollectingData(
        isMovingFlow: Flow<Boolean>,
        dataSource: () -> Flow<List<EmitterObservation<E, K>>>,
        mapMutex: Mutex,
        setter: (K, EmitterObservation<E, K>) -> Unit,
    ) {
        launch(Dispatchers.Default) {
            isMovingFlow
                .flatMapLatest { isMoving ->
                    if (isMoving) {
                        dataSource()
                    } else {
                        emptyFlow()
                    }
                }
                .collect { data ->
                    mapMutex.withLock { data.forEach { setter(it.emitter.uniqueKey, it) } }
                }
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

    fun getReportsFlow(
        scanSettings: ActiveScanSettings,
        onGpsActive: (Boolean) -> Unit,
    ): Flow<ReportData> {
        val batteryLevelOkFlow =
            if (scanSettings.lowBatteryThreshold == null) {
                flowOf(true)
            } else {
                batteryLevelMonitor.getBatteryLevelFlow().map { batteryPct ->
                    batteryPct.toPct() >= scanSettings.lowBatteryThreshold
                }
            }

        return batteryLevelOkFlow.flatMapLatest { batteryLevelOk ->
            if (batteryLevelOk) {
                createReports(scanSettings, onGpsActive)
            } else {
                emptyFlow()
            }
        }
    }

    private fun createReports(
        scanSettings: ActiveScanSettings,
        onGpsActive: (Boolean) -> Unit,
    ): Flow<ReportData> = channelFlow {
        val mutex = Mutex()

        val wifiAccessPointByMacAddr =
            mutableMapOf<MacAddress, EmitterObservation<WifiAccessPoint, MacAddress>>()
        val bluetoothBeaconsByMacAddr =
            mutableMapOf<MacAddress, EmitterObservation<BluetoothBeacon, MacAddress>>()
        val cellTowersByKey = mutableMapOf<String, EmitterObservation<CellTower, String>>()

        val isMovingFlow =
            movementDetectorProvider
                .getMovementDetector()
                .getIsMovingFlow()
                .stateIn(this, started = SharingStarted.WhileSubscribed(), initialValue = true)

        val locationFlow =
            locationSourceProvider
                .getLocationSource()
                .getLocations(LOCATION_INTERVAL, usePassiveProvider = false)
                .onStart { onGpsActive(true) }
                .onCompletion { onGpsActive(false) }
                .shareIn(this, started = SharingStarted.WhileSubscribed())

        val speedFlow =
            locationFlow
                .toSmoothenedSpeedFlow()
                .shareIn(this, started = SharingStarted.WhileSubscribed())

        startCollectingData(
            isMovingFlow,
            {
                wifiAccessPointSource.getWifiAccessPointFlow(
                    scanThrottled = !scanSettings.ignoreWifiScanThrottling,
                    scanInterval =
                        speedFlow.map { speed -> (speed / scanSettings.wifiScanDistance).seconds },
                )
            },
            mutex,
            wifiAccessPointByMacAddr::set,
        )

        startCollectingData(
            isMovingFlow,
            bluetoothBeaconSource::getBluetoothBeaconFlow,
            mutex,
            bluetoothBeaconsByMacAddr::set,
        )

        startCollectingData(
            isMovingFlow,
            {
                cellInfoSource.getCellInfoFlow(
                    interval =
                        speedFlow.map { speed -> (speed / scanSettings.cellScanDistance).seconds }
                )
            },
            mutex,
            cellTowersByKey::set,
        )

        val postProcessors = postProcessorProvider.getReportPostProcessors()

        isMovingFlow
            .flatMapLatest { isMoving ->
                if (isMoving) {
                    locationFlow.combineLocationsWithAirPressure()
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

                xyz.malkki.neostumbler.scanner.createReports(
                    locations,
                    cells,
                    wifis,
                    bluetooths,
                    postProcessors,
                )
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

    private fun Float.toPct(): Int {
        @Suppress("MagicNumber")
        return (this * 100).toInt()
    }
}
