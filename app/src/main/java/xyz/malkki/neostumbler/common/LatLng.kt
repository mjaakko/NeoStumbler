package xyz.malkki.neostumbler.common

/**
 * Simple value class describing a latitude/longitude pair
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        val ORIGIN = LatLng(0.0, 0.0)
    }

    fun isOrigin(): Boolean = this == ORIGIN

    fun asMapLibreLatLng(): org.maplibre.android.geometry.LatLng {
        return org.maplibre.android.geometry.LatLng(latitude, longitude)
    }
}
