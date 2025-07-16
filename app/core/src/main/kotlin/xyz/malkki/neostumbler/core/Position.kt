package xyz.malkki.neostumbler.core

import xyz.malkki.neostumbler.geography.LatLng

data class Position(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val altitude: Double? = null,
    val altitudeAccuracy: Double? = null,
    val heading: Double? = null,
    val speed: Double? = null,
    val pressure: Double? = null,
    val source: Source,
    /** Timestamp in milliseconds since boot */
    val timestamp: Long,
) {
    val latLng: LatLng
        get() = LatLng(latitude, longitude)

    enum class Source {
        /** Position received from a GNSS system (e.g. GPS) */
        GPS,
        /** Position received from a network location provider */
        NETWORK,
        /** Position received from a fused location provider (e.g. Google Play Services) */
        FUSED,
        /** Position with manually specified location */
        MANUAL,
    }
}
