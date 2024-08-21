package xyz.malkki.neostumbler.scanner.movement

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * Movement detector which only emits true
 */
object ConstantMovementDetector : MovementDetector {
    override fun getIsMovingFlow(): Flow<Boolean> = channelFlow {
        send(true)

        //Keep the flow active
        awaitClose {}
    }
}