package xyz.malkki.neostumbler.utils.geocoder

import android.location.Address
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import xyz.malkki.neostumbler.extensions.getFromLocationSuspending
import android.location.Geocoder as AndroidGeocoder

//Limit the amount of concurrent requests to avoid UI lag
private const val MAX_CONCURRENT_REQUESTS = 5

class PlatformGeocoder(private val geocoder: AndroidGeocoder, private val maxResults: Int = 1): Geocoder {
    private val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

    override suspend fun getAddresses(latitude: Double, longitude: Double): List<Address> = semaphore.withPermit {
        return geocoder.getFromLocationSuspending(latitude, longitude, maxResults)
    }
}