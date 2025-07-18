package xyz.malkki.neostumbler.data.emitter

import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.core.BluetoothBeacon

/** API for actively scanning Bluetooth beacons */
fun interface ActiveBluetoothBeaconSource {
    fun getBluetoothBeaconFlow(): Flow<List<BluetoothBeacon>>
}
