package xyz.malkki.neostumbler.domain

import xyz.malkki.neostumbler.geography.LatLng

fun LatLng.asMapLibreLatLng(): org.maplibre.android.geometry.LatLng {
    return org.maplibre.android.geometry.LatLng(latitude, longitude)
}

fun org.maplibre.android.geometry.LatLng.asDomainLatLng(): LatLng {
    return LatLng(latitude, longitude)
}
