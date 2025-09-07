package xyz.malkki.neostumbler.utils.geocoder

import java.util.Locale
import xyz.malkki.neostumbler.data.geocoder.Geocoder
import xyz.malkki.neostumbler.geography.LatLng

/** Stub implementation of [Geocoder], returns `null` for all queries */
object StubGeocoder : Geocoder {
    override suspend fun getAddress(locale: Locale, latLng: LatLng): String? = null
}
