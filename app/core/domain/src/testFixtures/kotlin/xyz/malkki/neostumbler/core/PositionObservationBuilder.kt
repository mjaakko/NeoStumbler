package xyz.malkki.neostumbler.core

import xyz.malkki.neostumbler.core.observation.PositionObservation

fun somePositionObservation(
    position: Position = somePosition(),
    timestamp: Long = 0,
) = PositionObservation(position = position, timestamp = timestamp)
