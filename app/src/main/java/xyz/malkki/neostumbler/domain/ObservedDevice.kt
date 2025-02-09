package xyz.malkki.neostumbler.domain

/** Interface for observations of wireless devices */
interface ObservedDevice<K> {
    val uniqueKey: K

    /** Timestamp when the observation was made */
    val timestamp: Long
}
