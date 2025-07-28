package xyz.malkki.neostumbler.core.observation

import xyz.malkki.neostumbler.core.Position

data class PositionObservation(
    val position: Position,
    /** Time when the observation was made (in milliseconds since boot) */
    val timestamp: Long,
)
