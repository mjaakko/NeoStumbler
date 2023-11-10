package xyz.malkki.neostumbler.common

import android.location.Location

data class LocationWithSource(val location: Location, val source: LocationSource) {
    enum class LocationSource {
        FUSED, GPS
    }
}
