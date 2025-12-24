package xyz.malkki.neostumbler.core.emitter

/** Radio signal emitter */
sealed interface Emitter<K> {
    /** Unique identifier of the emitter */
    val uniqueKey: K

    val signalStrength: Int?
}
