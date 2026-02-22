package xyz.malkki.neostumbler.scanner.movement

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import xyz.malkki.neostumbler.data.movement.MovementDetector

/** Movement detector which only emits true (i.e. always moving) */
object ConstantMovementDetector : MovementDetector {
    override fun getIsMovingFlow(): Flow<Boolean> = channelFlow {
        send(true)

        // Keep the flow active
        awaitClose {}
    }
}
