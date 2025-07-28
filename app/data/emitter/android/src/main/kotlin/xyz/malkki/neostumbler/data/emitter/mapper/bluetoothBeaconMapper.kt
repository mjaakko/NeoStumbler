package xyz.malkki.neostumbler.data.emitter.mapper

import android.os.SystemClock
import org.altbeacon.beacon.Beacon
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.observation.EmitterObservation

internal fun Beacon.toBluetoothBeacon(): EmitterObservation<BluetoothBeacon, MacAddress> {
    val timestamp =
        SystemClock.elapsedRealtime() - (System.currentTimeMillis() - lastCycleDetectionTimestamp)

    return EmitterObservation(
        emitter =
            BluetoothBeacon(
                macAddress = MacAddress(bluetoothAddress),
                beaconType = beaconTypeCode,
                id1 = id1?.toString(),
                id2 = id2?.toString(),
                id3 = id3?.toString(),
                signalStrength = rssi,
            ),
        timestamp = timestamp,
    )
}
