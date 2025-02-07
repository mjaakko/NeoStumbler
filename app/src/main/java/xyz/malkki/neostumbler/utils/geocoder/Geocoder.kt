package xyz.malkki.neostumbler.utils.geocoder

import android.location.Address

interface Geocoder {
    suspend fun getAddresses(latitude: Double, longitude: Double): List<Address>
}
