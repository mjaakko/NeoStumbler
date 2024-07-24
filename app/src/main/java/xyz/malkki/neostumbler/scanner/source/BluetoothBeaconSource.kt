package xyz.malkki.neostumbler.scanner.source

import kotlinx.coroutines.flow.Flow
import xyz.malkki.neostumbler.domain.BluetoothBeacon

interface BluetoothBeaconSource {
    fun getBluetoothBeaconFlow(): Flow<List<BluetoothBeacon>>
}