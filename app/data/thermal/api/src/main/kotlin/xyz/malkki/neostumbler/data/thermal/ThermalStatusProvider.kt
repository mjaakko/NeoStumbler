package xyz.malkki.neostumbler.data.thermal

import kotlinx.coroutines.flow.Flow

fun interface ThermalStatusProvider {
    /**
     * Returns a flow describing device overheating status.
     * * `true` when the device is overheating
     * * `false` when not
     */
    fun getIsDeviceOverheatingFlow(): Flow<Boolean>
}
