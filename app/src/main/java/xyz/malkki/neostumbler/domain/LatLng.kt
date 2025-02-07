package xyz.malkki.neostumbler.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Simple value class describing a latitude/longitude pair */
data class LatLng(val latitude: Double, val longitude: Double) {
    companion object {
        private const val EARTH_RADIUS_IN_METERS = 6371 * 1000

        val ORIGIN = LatLng(0.0, 0.0)
    }

    fun isOrigin(): Boolean = this == ORIGIN

    fun asMapLibreLatLng(): org.maplibre.android.geometry.LatLng {
        return org.maplibre.android.geometry.LatLng(latitude, longitude)
    }

    /**
     * Calculates distance to the other coordinate
     *
     * @return Distance in meters
     */
    fun distanceTo(other: LatLng): Double {
        val latDistance = Math.toRadians(other.latitude - latitude)
        val lonDistance = Math.toRadians(other.longitude - longitude)

        val a =
            (sin(latDistance / 2) * sin(latDistance / 2) +
                (cos(Math.toRadians(latitude)) *
                    cos(Math.toRadians(other.latitude)) *
                    sin(lonDistance / 2) *
                    sin(lonDistance / 2)))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_IN_METERS * c
    }
}
