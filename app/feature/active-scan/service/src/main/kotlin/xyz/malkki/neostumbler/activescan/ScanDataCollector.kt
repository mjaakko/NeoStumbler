package xyz.malkki.neostumbler.activescan

import androidx.collection.mutableLongObjectMapOf
import androidx.collection.mutableScatterMapOf
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.data.emitter.ActiveBluetoothBeaconSource
import xyz.malkki.neostumbler.data.emitter.ActiveCellInfoSource
import xyz.malkki.neostumbler.data.emitter.ActiveWifiAccessPointSource

internal class ScanDataCollector(
    private val isMovingFlow: Flow<Boolean>,
    private val speedFlow: Flow<Double>,
    private val wifiSource: ActiveWifiAccessPointSource,
    private val cellSource: ActiveCellInfoSource,
    private val bluetoothBeaconSource: ActiveBluetoothBeaconSource,
    private val scanSettings: ActiveScanSettings,
    coroutineScope: CoroutineScope,
) {
    private val wifiAccessPointByMacAddress =
        mutableLongObjectMapOf<EmitterObservation<WifiAccessPoint, MacAddress>>()
    private val bluetoothBeaconByMacAddress =
        mutableLongObjectMapOf<EmitterObservation<BluetoothBeacon, MacAddress>>()
    private val cellTowerByKey =
        mutableScatterMapOf<String, EmitterObservation<CellTower, String>>()

    private val mutex = Mutex()

    init {
        coroutineScope.launch {
            isMovingFlow
                .flatMapLatest { isMoving ->
                    if (isMoving) {
                        wifiSource.getWifiAccessPointFlow(
                            scanThrottled = !scanSettings.ignoreWifiScanThrottling,
                            scanInterval =
                                speedFlow.map { speed ->
                                    (speed / scanSettings.wifiScanDistance).seconds
                                },
                        )
                    } else {
                        emptyFlow()
                    }
                }
                .collect { wifiAccessPoints ->
                    mutex.withLock {
                        wifiAccessPoints.forEach {
                            wifiAccessPointByMacAddress[it.emitter.uniqueKey.raw] = it
                        }
                    }
                }
        }

        coroutineScope.launch {
            isMovingFlow
                .flatMapLatest { isMoving ->
                    if (isMoving) {
                        bluetoothBeaconSource.getBluetoothBeaconFlow()
                    } else {
                        emptyFlow()
                    }
                }
                .collect { bluetoothBeacons ->
                    mutex.withLock {
                        bluetoothBeacons.forEach {
                            bluetoothBeaconByMacAddress[it.emitter.uniqueKey.raw] = it
                        }
                    }
                }
        }

        coroutineScope.launch {
            isMovingFlow
                .flatMapLatest { isMoving ->
                    if (isMoving) {
                        cellSource.getCellInfoFlow(
                            interval =
                                speedFlow.map { speed ->
                                    (speed / scanSettings.cellScanDistance).seconds
                                }
                        )
                    } else {
                        emptyFlow()
                    }
                }
                .collect { cellTowers ->
                    mutex.withLock {
                        cellTowers.forEach { cellTowerByKey[it.emitter.uniqueKey] = it }
                    }
                }
        }
    }

    suspend fun getCollectedData():
        Triple<
            List<EmitterObservation<WifiAccessPoint, MacAddress>>,
            List<EmitterObservation<BluetoothBeacon, MacAddress>>,
            List<EmitterObservation<CellTower, String>>,
        > = mutex.withLock {
        val wifiAccessPoints =
            ArrayList<EmitterObservation<WifiAccessPoint, MacAddress>>(
                wifiAccessPointByMacAddress.size
            )
        wifiAccessPointByMacAddress.forEachValue { wifiAccessPoints.add(it) }
        wifiAccessPointByMacAddress.clear()

        val bluetoothBeacons =
            ArrayList<EmitterObservation<BluetoothBeacon, MacAddress>>(
                bluetoothBeaconByMacAddress.size
            )
        bluetoothBeaconByMacAddress.forEachValue { bluetoothBeacons.add(it) }
        bluetoothBeaconByMacAddress.clear()

        val cellTowers = ArrayList<EmitterObservation<CellTower, String>>(cellTowerByKey.size)
        cellTowerByKey.forEachValue { cellTowers.add(it) }
        cellTowerByKey.clear()

        Triple(wifiAccessPoints, bluetoothBeacons, cellTowers)
    }
}
