package xyz.malkki.neostumbler.domain

/**
 * Interface for observations of wireless devices
 */
interface ObservedDevice {
    /**
     * Timestamp when the observation was made
     */
    val timestamp: Long
}