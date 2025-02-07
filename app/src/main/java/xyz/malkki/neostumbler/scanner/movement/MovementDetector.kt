package xyz.malkki.neostumbler.scanner.movement

import kotlinx.coroutines.flow.Flow

fun interface MovementDetector {
    /** @return A flow which emits true, when the device is moving, and false, when it's not */
    fun getIsMovingFlow(): Flow<Boolean>
}
