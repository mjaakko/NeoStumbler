package xyz.malkki.neostumbler.core

import xyz.malkki.neostumbler.core.values.Speed

fun somePosition(
    latitude: Double = 56.414156,
    longitude: Double = 18.724728,
    speed: Speed = Speed(5.6378),
) =
    Position(
        latitude = latitude,
        longitude = longitude,
        accuracy = 15.4,
        altitude = null,
        altitudeAccuracy = null,
        heading = 14.516,
        pressure = null,
        speed = speed,
        source = Position.Source.GPS,
    )
