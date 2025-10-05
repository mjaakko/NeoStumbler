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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import timber.log.Timber
import xyz.malkki.neostumbler.beaconparser.BeaconLayout.Companion.parseBeaconLayout
import xyz.malkki.neostumbler.beaconparser.BeaconParser
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.createScanFilters
import xyz.malkki.neostumbler.data.emitter.internal.getDeviceInteractiveFlow

private const val MS_IN_NS = 1_000_000

private val THROTTLE_RETRY_DELAY = 5.seconds

class BLEScannerBluetoothBeaconSource(context: Context) : ActiveBluetoothBeaconSource {
    companion object {
        // These could be defined in a text file
        private val BEACON_LAYOUTS =
            listOf(
                    // AltBeacon
                    "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25",
                    // iBeacon
                    "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24",
                    // Eddystone-UID
                    "s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19",
                    // RuuviTag v5
                    // https://docs.ruuvi.com/communication/bluetooth-advertisements/data-format-5-rawv2
                    "m:0-2=990405,i:20-25",
                )
                .map { it.parseBeaconLayout() }

        private val BEACON_PARSERS = BEACON_LAYOUTS.map { BeaconParser(it) }

        /**
         * List of known beacon manufacturer IDs. These are used for scan filters when the screen is
         * off as otherwise Android does not allow active Bluetooth scanning
         */
        private val KNOWN_BEACON_MANUFACTURERS = listOf(0x04c, 0x0118)

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
                                id1 = beaconData.identifiers.getOrNull(0)?.toString(),
                                id2 = beaconData.identifiers.getOrNull(1)?.toString(),
                                id3 = beaconData.identifiers.getOrNull(2)?.toString(),
                                signalStrength = scanResult.rssi,
                            ),
                        timestamp = scanResult.timestampNanos / MS_IN_NS,
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

        return callbackFlow {
                val callback =
                    object : ScanCallback() {
                        override fun onBatchScanResults(results: List<ScanResult>?) {
                            results?.let {
                                it.forEach { scanResult -> trySendBlocking(scanResult) }
                            }
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
            .retry { ex ->
                if (ex is BluetoothScanException) {
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
