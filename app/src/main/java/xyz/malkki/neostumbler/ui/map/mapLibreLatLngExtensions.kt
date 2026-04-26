package xyz.malkki.neostumbler.ui.map

import kotlin.math.abs
import org.maplibre.android.geometry.LatLng

private const val MAX_COORDINATE_DIFF = 0.00001

internal fun LatLng.isCloseTo(other: LatLng): Boolean {
    return abs(latitude - other.latitude) < MAX_COORDINATE_DIFF &&
        abs(longitude - other.longitude) < MAX_COORDINATE_DIFF
}

internal fun LatLng.isCloseToOrigin(): Boolean {
    return abs(latitude) < MAX_COORDINATE_DIFF && abs(longitude) < MAX_COORDINATE_DIFF
}
