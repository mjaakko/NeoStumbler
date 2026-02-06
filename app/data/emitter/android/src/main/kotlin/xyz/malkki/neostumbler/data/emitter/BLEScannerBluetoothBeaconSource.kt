package xyz.malkki.neostumbler.data.emitter

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.timeout
import timber.log.Timber
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.BluetoothBeaconConstants.BEACON_LAYOUTS
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.BluetoothBeaconConstants.BEACON_PARSERS
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.BluetoothBeaconConstants.KNOWN_BEACON_MANUFACTURERS
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.createScanFilters
import xyz.malkki.neostumbler.data.emitter.internal.timestampMillis
import xyz.malkki.neostumbler.data.emitter.internal.util.getDeviceInteractiveFlow

private val THROTTLE_RETRY_DELAY = 5.seconds

// Timeout after which to restart scanning if no results are received
private val SCAN_RESULT_TIMEOUT = 10.seconds

private const val SCAN_RESULT_BUFFER_SIZE = 10

class BLEScannerBluetoothBeaconSource(context: Context) : ActiveBluetoothBeaconSource {
    companion object {

        private val SCAN_SETTINGS =
            ScanSettings.Builder()
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
    }

    private val appContext = context.applicationContext

    private val bluetoothManager = appContext.getSystemService<BluetoothManager>()!!

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    override fun getBluetoothBeaconFlow():
        Flow<List<EmitterObservation<BluetoothBeacon, MacAddress>>> {
        return appContext
            .getDeviceInteractiveFlow()
            .flatMapLatest { isInteractive ->
                val filters =
                    if (!isInteractive) {
                        BEACON_LAYOUTS.flatMap {
                            it.createScanFilters(manufacturerIds = KNOWN_BEACON_MANUFACTURERS)
                        }
                    } else {
                        listOf(ScanFilter.Builder().build())
                    }

                getBluetoothScanFlow(scanSettings = SCAN_SETTINGS, filters = filters)
            }
            .map { scanResult ->
                val beaconDataList =
                    scanResult.scanRecord?.bytes?.let { scanData ->
                        BEACON_PARSERS.mapNotNull { beaconParser ->
                            beaconParser.parseScanData(scanData)
                        }
                    }

                beaconDataList?.map { beaconData ->
                    EmitterObservation(
                        emitter =
                            BluetoothBeacon(
                                macAddress = MacAddress(scanResult.device.address),
                                beaconType = beaconData.beaconType,
                                id1 =
                                    if (beaconData.identifiers.size > 0) {
                                        beaconData.identifiers[0].toString()
                                    } else {
                                        null
                                    },
                                id2 =
                                    if (beaconData.identifiers.size > 1) {
                                        beaconData.identifiers[1].toString()
                                    } else {
                                        null
                                    },
                                id3 =
                                    if (beaconData.identifiers.size > 2) {
                                        beaconData.identifiers[2].toString()
                                    } else {
                                        null
                                    },
                                signalStrength = scanResult.rssi,
                            ),
                        timestamp = scanResult.timestampMillis,
                    )
                } ?: emptyList()
            }
            .filter { it.isNotEmpty() }
    }

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN)
    private fun getBluetoothScanFlow(
        scanSettings: ScanSettings,
        filters: List<ScanFilter>? = null,
    ): Flow<ScanResult> {
        val bleScanner = bluetoothManager.adapter?.bluetoothLeScanner ?: return emptyFlow()

        val bluetoothScanFlow = callbackFlow {
            val callback =
                object : ScanCallback() {
                    override fun onBatchScanResults(results: List<ScanResult>?) {
                        results?.let { it.forEach { scanResult -> trySendBlocking(scanResult) } }
                    }

                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        result?.let { trySendBlocking(it) }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        close(BluetoothScanException(errorCode))
                    }
                }

            bleScanner.startScan(filters, scanSettings, callback)

            awaitClose { bleScanner.stopScan(callback) }
        }

        return bluetoothScanFlow
            .buffer(
                capacity = SCAN_RESULT_BUFFER_SIZE,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
            .timeout(SCAN_RESULT_TIMEOUT)
            .retry { ex ->
                if (ex is TimeoutCancellationException) {
                    Timber.i(
                        "No Bluetooth scan results received in %s, restarting scanning..",
                        SCAN_RESULT_TIMEOUT.toString(),
                    )
                } else if (ex is BluetoothScanException) {
                    Timber.w("Bluetooth scan failed: %d", ex.errorCode)

                    if (
                        ex.errorCode == ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY ||
                            ex.errorCode == ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES
                    ) {
                        Timber.i(
                            "Scanning too often, retrying after %s",
                            THROTTLE_RETRY_DELAY.toString(),
                        )

                        delay(THROTTLE_RETRY_DELAY)
                    }
                }

                true
            }
    }

    private class BluetoothScanException(val errorCode: Int) : Exception()
}
