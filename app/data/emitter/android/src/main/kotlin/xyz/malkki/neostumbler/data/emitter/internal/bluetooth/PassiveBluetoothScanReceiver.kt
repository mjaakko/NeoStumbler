package xyz.malkki.neostumbler.data.emitter.internal.bluetooth

import android.app.PendingIntent
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import androidx.core.app.PendingIntentCompat
import androidx.core.content.IntentCompat
import xyz.malkki.neostumbler.coroutinebroadcastreceiver.CoroutineBroadcastReceiver
import xyz.malkki.neostumbler.data.emitter.internal.bluetooth.PassiveBluetoothBeaconScanResult.Identifier.Companion.toStoreIdentifier
import xyz.malkki.neostumbler.data.emitter.internal.timestampMillis

internal class PassiveBluetoothScanReceiver : CoroutineBroadcastReceiver() {
    companion object {
        private const val REQUEST_CODE = 700000

        fun getPendingIntent(context: Context): PendingIntent {
            val intent =
                Intent(context.applicationContext, PassiveBluetoothScanReceiver::class.java)

            return PendingIntentCompat.getBroadcast(
                context.applicationContext,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT,
                true,
            )!!
        }
    }

    override suspend fun handleIntent(context: Context, intent: Intent) {
        val scanResults: List<ScanResult>? =
            IntentCompat.getParcelableArrayListExtra(
                intent,
                BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
                ScanResult::class.java,
            )

        if (scanResults != null) {
            val beaconScanResults =
                BluetoothBeaconConstants.BEACON_PARSERS.flatMap { parser ->
                    scanResults.mapNotNull { scanResult ->
                        parser.parseScanData(scanResult.scanRecord!!.bytes)?.let { beaconData ->
                            PassiveBluetoothBeaconScanResult(
                                address = scanResult.device.address,
                                timestamp = scanResult.timestampMillis,
                                signalStrength = scanResult.rssi,
                                beaconType = beaconData.beaconType,
                                identifiers = beaconData.identifiers.map { it.toStoreIdentifier() },
                            )
                        }
                    }
                }

            val scanResultStore = PassiveBluetoothScanResultStore(context)
            scanResultStore.save(beaconScanResults)
        }
    }
}
