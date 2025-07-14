package xyz.malkki.neostumbler.ichnaea.dto

import kotlinx.serialization.Serializable

@Serializable
data class GeolocateResponseDto(val location: LocationDto, val accuracy: Double) {
    @Serializable data class LocationDto(val lat: Double, val lng: Double)
}
