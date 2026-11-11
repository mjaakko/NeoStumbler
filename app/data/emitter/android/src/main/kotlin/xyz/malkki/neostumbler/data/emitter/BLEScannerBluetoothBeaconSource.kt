package xyz.malkki.neostumbler.data.emitter

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.malkki.neostumbler.beaconparser.BeaconData
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.core.values.SignalStrength
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.BluetoothBeaconConstants.BEACON_LAYOUTS
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.BluetoothBeaconConstants.BEACON_PARSERS
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.BluetoothBeaconConstants.KNOWN_BEACON_MANUFACTURERS
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.createScanFilters
import xyz.malkki.neostumbler.data.emitter.internal.timestampMillis
import xyz.malkki.neostumbler.data.emitter.internal.util.delayWithMinDuration
import xyz.malkki.neostumbler.data.emitter.internal.util.getDeviceInteractiveFlow

private val THROTTLE_RETRY_DELAY = 5.seconds

// Timeout after which to restart scanning if no results are received
private val SCAN_RESULT_TIMEOUT = 10.seconds

/**
 * Duration of an individual BLE scan.
 *
 * Typically, beacons send their advertisement once per second so this should be long enough to
 * detect them.
 */
private val SCAN_DURATION = 2.seconds

/**
 * Android throttles Bluetooth scans when starting more than 5 scans per 30 second duration.
 *
 * To avoid starting too many scans, just keep the existing scan active if it's not possible to
 * sleep at least 4 seconds between scans. (4 + 2) * 5 = 30
 */
private val MIN_SLEEP_DURATION = 4.seconds

private const val SCAN_RESULT_BUFFER_SIZE = 10

class BLEScannerBluetoothBeaconSource(
    context: Context,
    private val timeSource: () -> Long = SystemClock::elapsedRealtime,
) : ActiveBluetoothBeaconSource {
    companion object {
        private val SCAN_SETTINGS =
            ScanSettings.Builder()
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
    }

    private val appContext = context.applicationContext

    private val bluetoothManager = appContext.getSystemService<BluetoothManager>()!!

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun getBluetoothBeaconFlow(
        scanInterval: Flow<Duration>
    ): Flow<List<EmitterObservation<BluetoothBeacon, MacAddress>>> = channelFlow {
        if (!appContext.hasBluetoothScanPermission()) {
            Timber.w("No Bluetooth scan permission, not scanning for Bluetooth beacons")

            close()
            return@channelFlow
        }

        val scanInterval = scanInterval.stateIn(this)

        val scanActive = MutableStateFlow(true)

        launch(Dispatchers.Default) {
            while (true) {
                scanActive.value = true

                delay(SCAN_DURATION)

                if (scanInterval.value > MIN_SLEEP_DURATION) {
                    scanActive.value = false

                    delayWithMinDuration(
                        startTime = timeSource.invoke(),
                        timeSource = timeSource,
                        durationFlow = scanInterval,
                    )
                }
            }
        }

        launch(Dispatchers.Default) {
            scanActive.collectLatest { scanActive ->
                if (scanActive) {
                    appContext
                        .getDeviceInteractiveFlow()
                        .flatMapLatest { isInteractive ->
                            val filters =
                                if (!isInteractive) {
                                    BEACON_LAYOUTS.flatMap {
                                        it.createScanFilters(
                                            manufacturerIds = KNOWN_BEACON_MANUFACTURERS
                                        )
                                    }
                                } else {
                                    listOf(ScanFilter.Builder().build())
                                }

                            getBluetoothScanFlow(scanSettings = SCAN_SETTINGS, filters = filters)
                        }
                        .map { scanResult ->
                            val beaconDataList =
                                scanResult.scanRecord
                                    ?.bytes
                                    ?.let { scanData ->
                                        BEACON_PARSERS.mapNotNull { beaconParser ->
                                            beaconParser.parseScanData(scanData)
                                        }
                                    }
                                    .orEmpty()

                            beaconDataList.map { beaconData ->
                                beaconData.toEmitterObservation(scanResult)
                            }
                        }
                        .filter { it.isNotEmpty() }
                        .collect { observations -> send(observations) }
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
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

            Timber.d("Starting BLE scanning")
            bleScanner.startScan(filters, scanSettings, callback)

            awaitClose {
                Timber.d("Stopping BLE scanning")

                bleScanner.stopScan(callback)
            }
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

private fun Context.hasBluetoothScanPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) ==
                PackageManager.PERMISSION_GRANTED
    }
}

private fun BeaconData.toEmitterObservation(scanResult: ScanResult) =
    EmitterObservation(
        emitter =
            BluetoothBeacon(
                macAddress = MacAddress(scanResult.device.address),
                beaconType = beaconType,
                id1 =
                    if (identifiers.size > 0) {
                        identifiers[0].toString()
                    } else {
                        null
                    },
                id2 =
                    if (identifiers.size > 1) {
                        identifiers[1].toString()
                    } else {
                        null
                    },
                id3 =
                    if (identifiers.size > 2) {
                        identifiers[2].toString()
                    } else {
                        null
                    },
                signalStrength = SignalStrength(scanResult.rssi),
            ),
        timestamp = scanResult.timestampMillis,
    )
