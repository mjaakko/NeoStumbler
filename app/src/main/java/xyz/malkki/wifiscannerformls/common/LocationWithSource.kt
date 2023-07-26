package xyz.malkki.wifiscannerformls.common

import android.location.Location

data class LocationWithSource(val location: Location, val source: LocationSource) {
    enum class LocationSource {
        FUSED, GPS
    }
}
