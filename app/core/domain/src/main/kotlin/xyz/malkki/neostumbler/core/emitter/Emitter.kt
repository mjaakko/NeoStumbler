package xyz.malkki.neostumbler.core.emitter

import xyz.malkki.neostumbler.core.values.SignalStrength

/** Radio signal emitter */
sealed interface Emitter<K> {
    /** Unique identifier of the emitter */
    val uniqueKey: K

    val signalStrength: SignalStrength?
}
