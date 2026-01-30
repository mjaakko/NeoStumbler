package xyz.malkki.neostumbler.data.emitter

import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.observation.EmitterObservation

interface PassiveBluetoothBeaconSource {
    /** Enables opportunistic Bluetooth scanner to collect Bluetooth beacons passively */
    fun enable()

    /** Disables opportunistic Bluetooth scanner */
    fun disable()

    /**
     * Returns Bluetooth beacons collected by the opportunistic Bluetooth scanner
     *
     * Note that this only returns data when the opportunistic scanner has been enabled, see
     * [enable]
     */
    suspend fun getBluetoothBeacons(): List<EmitterObservation<BluetoothBeacon, MacAddress>>
}
