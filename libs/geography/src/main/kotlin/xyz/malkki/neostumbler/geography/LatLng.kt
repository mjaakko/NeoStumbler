package xyz.malkki.neostumbler.geography

import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.asin
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

    /**
     * Calculates distance to the other coordinate
     *
     * @return Distance in meters
     */
    fun distanceTo(other: LatLng): Double {
        val latDistance = toRadians(other.latitude - latitude)
        val lonDistance = toRadians(other.longitude - longitude)

        val a =
            (sin(latDistance / 2) * sin(latDistance / 2) +
                (cos(Math.toRadians(latitude)) *
                    cos(Math.toRadians(other.latitude)) *
                    sin(lonDistance / 2) *
                    sin(lonDistance / 2)))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_IN_METERS * c
    }

    /**
     * Calculates destination coordinate when traveling from this coordinate
     *
     * @param distance Distance in metres
     * @param bearing Bearing in degrees from north
     */
    fun destination(distance: Double, bearing: Double): LatLng {
        val angDist = distance / EARTH_RADIUS_IN_METERS

        val latitudeRadians = toRadians(latitude)
        val longitudeRadians = toRadians(longitude)

        val bearingRadians = toRadians(bearing)

        val lat2 =
            asin(
                sin(latitudeRadians) * cos(angDist) +
                    cos(latitudeRadians) * sin(angDist) * cos(bearingRadians)
            )
        val lon2 =
            longitudeRadians +
                atan2(
                    sin(bearingRadians) * sin(angDist) * cos(latitudeRadians),
                    cos(angDist) - sin(latitudeRadians) * sin(lat2),
                )

        @Suppress("MagicNumber")
        return LatLng(toDegrees(lat2), ((toDegrees(lon2) + 540) % 360) - 180)
    }
}
