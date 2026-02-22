package xyz.malkki.neostumbler.data.emitter

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.observation.EmitterObservation
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.PassiveBluetoothScanReceiver
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.PassiveBluetoothScanResultStore

class BLEScannerPassiveBluetoothBeaconSource(context: Context) : PassiveBluetoothBeaconSource {
    companion object {
        private val PASSIVE_SCAN_SETTINGS =
            ScanSettings.Builder()
                // Note: opportunistic scan mode does not seem to work with report delay
                .setScanMode(ScanSettings.SCAN_MODE_OPPORTUNISTIC)
                .build()
    }

    private val appContext: Context = context.applicationContext

    private val bluetoothManager: BluetoothManager =
        appContext.getSystemService<BluetoothManager>()!!

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun enable() {
        bluetoothManager.adapter
            ?.bluetoothLeScanner
            ?.startScan(
                null,
                PASSIVE_SCAN_SETTINGS,
                PassiveBluetoothScanReceiver.getPendingIntent(appContext),
            )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun disable() {
        bluetoothManager.adapter
            ?.bluetoothLeScanner
            ?.stopScan(PassiveBluetoothScanReceiver.getPendingIntent(appContext))
    }

    override suspend fun getBluetoothBeacons():
        List<EmitterObservation<BluetoothBeacon, MacAddress>> {
        val store = PassiveBluetoothScanResultStore(appContext)

        return store.get().map { scanResult ->
            val mappedIdentifiers = scanResult.identifiers.map { it.toBeaconDataIdentifier() }

            EmitterObservation(
                emitter =
                    BluetoothBeacon(
                        macAddress = MacAddress(scanResult.address),
                        beaconType = scanResult.beaconType,
                        id1 = mappedIdentifiers.getOrNull(0)?.toString(),
                        id2 = mappedIdentifiers.getOrNull(1)?.toString(),
                        id3 = mappedIdentifiers.getOrNull(2)?.toString(),
                        signalStrength = scanResult.signalStrength,
                    ),
                timestamp = scanResult.timestamp,
            )
        }
    }
}
