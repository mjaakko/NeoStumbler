package xyz.malkki.neostumbler.data.geocoder

import java.util.Locale
import xyz.malkki.neostumbler.geography.LatLng

interface Geocoder {
    /** Finds a human-readable address for the given coordinates */
    suspend fun getAddress(locale: Locale, latLng: LatLng): String?
}
