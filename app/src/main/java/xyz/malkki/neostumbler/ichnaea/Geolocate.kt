package xyz.malkki.neostumbler.ichnaea

import xyz.malkki.neostumbler.ichnaea.dto.GeolocateRequestDto
import xyz.malkki.neostumbler.ichnaea.dto.GeolocateResponseDto

interface Geolocate {
    suspend fun getLocation(requestDto: GeolocateRequestDto): GeolocateResponseDto
}
