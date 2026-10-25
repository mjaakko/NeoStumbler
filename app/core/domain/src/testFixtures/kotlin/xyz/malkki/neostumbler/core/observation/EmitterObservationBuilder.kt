package xyz.malkki.neostumbler.core.observation

import xyz.malkki.neostumbler.core.emitter.Emitter
import xyz.malkki.neostumbler.core.ranging.EstimatedDistance

fun <E : Emitter<K>, K> someEmitterObservation(
    emitter: E,
    timestamp: Long = 0,
    estimatedDistance: EstimatedDistance? = null,
) =
    EmitterObservation(
        emitter = emitter,
        timestamp = timestamp,
        estimatedDistance = estimatedDistance,
    )
