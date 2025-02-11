package xyz.malkki.neostumbler.scanner.source

import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.domain.BluetoothBeacon

fun interface BluetoothBeaconSource {
    fun getBluetoothBeaconFlow(): Flow<List<BluetoothBeacon>>
}
