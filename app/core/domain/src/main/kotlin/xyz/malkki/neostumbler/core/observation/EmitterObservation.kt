package xyz.malkki.neostumbler.core.observation

import xyz.malkki.neostumbler.core.emitter.Emitter
import xyz.malkki.neostumbler.core.ranging.EstimatedDistance

data class EmitterObservation<E : Emitter<K>, K>(
    val emitter: E,
    /** Time when the observation was made (in milliseconds since boot) */
    val timestamp: Long,
    /** Estimated distance to the emitter. `null` if unknown */
    val estimatedDistance: EstimatedDistance? = null,
)
