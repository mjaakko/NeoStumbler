package xyz.malkki.neostumbler.domain

import android.os.SystemClock
import org.altbeacon.beacon.Beacon

data class BluetoothBeacon(
    val macAddress: String,
    val beaconType: Int? = null,
    val id1: String? = null,
    val id2: String? = null,
    val id3: String? = null,
    val signalStrength: Int? = null,
    override val timestamp: Long,
) : ObservedDevice<String> {
    override val uniqueKey: String
        get() = macAddress

    companion object {
        fun fromBeacon(beacon: Beacon): BluetoothBeacon {
            val timestamp =
                SystemClock.elapsedRealtime() -
                    (System.currentTimeMillis() - beacon.lastCycleDetectionTimestamp)

            return BluetoothBeacon(
                macAddress = beacon.bluetoothAddress,
                beaconType = beacon.beaconTypeCode,
                id1 = beacon.id1?.toString(),
                id2 = beacon.id2?.toString(),
                id3 = beacon.id3?.toString(),
                signalStrength = beacon.rssi,
                timestamp = timestamp,
            )
        }
    }
}
