package xyz.malkki.neostumbler.core.observation

import xyz.malkki.neostumbler.core.emitter.Emitter

data class EmitterObservation<E : Emitter<K>, K>(
    val emitter: E,
    /** Time when the observation was made (in milliseconds since boot) */
    val timestamp: Long,
)
