package xyz.malkki.neostumbler.ichnaea.dto

import xyz.malkki.neostumbler.domain.LatLng

val GeolocateResponseDto.LocationDto.latLng: LatLng
    get() = LatLng(lat, lng)
