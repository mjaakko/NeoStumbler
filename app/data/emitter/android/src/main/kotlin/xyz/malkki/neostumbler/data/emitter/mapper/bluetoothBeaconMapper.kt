package xyz.malkki.neostumbler.data.emitter.mapper

import android.os.SystemClock
import org.altbeacon.beacon.Beacon
import xyz.malkki.neostumbler.core.BluetoothBeacon

internal fun Beacon.toBluetoothBeacon(): BluetoothBeacon {
    val timestamp =
        SystemClock.elapsedRealtime() - (System.currentTimeMillis() - lastCycleDetectionTimestamp)

    return BluetoothBeacon(
        macAddress = bluetoothAddress,
        beaconType = beaconTypeCode,
        id1 = id1?.toString(),
        id2 = id2?.toString(),
        id3 = id3?.toString(),
        signalStrength = rssi,
        timestamp = timestamp,
    )
}
