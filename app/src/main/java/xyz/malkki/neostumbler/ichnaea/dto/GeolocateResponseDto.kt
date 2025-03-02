package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable
import xyz.malkki.neostumbler.domain.LatLng

@Serializable
data class GeolocateResponseDto(val location: LocationDto, val accuracy: Double) {
    @Serializable
    data class LocationDto(val lat: Double, val lng: Double) {
        val latLng: LatLng
            get() = LatLng(lat, lng)
    }
}
