package xyz.malkki.neostumbler.ichnaea.dto

import xyz.malkki.neostumbler.geography.LatLng

val GeolocateResponseDto.LocationDto.latLng: LatLng
    get() = LatLng(lat, lng)
