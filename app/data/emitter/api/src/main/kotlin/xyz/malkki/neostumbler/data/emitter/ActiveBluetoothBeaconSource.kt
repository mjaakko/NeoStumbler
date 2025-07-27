package xyz.malkki.neostumbler.data.emitter

import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.observation.EmitterObservation

/** API for actively scanning Bluetooth beacons */
fun interface ActiveBluetoothBeaconSource {
    fun getBluetoothBeaconFlow(): Flow<List<EmitterObservation<BluetoothBeacon, MacAddress>>>
}
