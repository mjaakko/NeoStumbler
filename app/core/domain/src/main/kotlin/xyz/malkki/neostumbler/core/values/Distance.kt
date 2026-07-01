package xyz.malkki.neostumbler.core.values

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@JvmInline
value class Distance(val meters: Double) {
    operator fun div(speed: Speed): Duration {
        return (meters / speed.metersPerSecond).seconds
    }
}
