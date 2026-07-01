package xyz.malkki.neostumbler.core.values

@JvmInline
value class Speed(val metersPerSecond: Double) {
    operator fun times(multiplier: Double): Speed {
        return Speed(metersPerSecond * multiplier)
    }

    operator fun plus(other: Speed): Speed {
        return Speed(metersPerSecond + other.metersPerSecond)
    }
}
