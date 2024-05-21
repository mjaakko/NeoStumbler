package xyz.malkki.neostumbler.utils.geocoder

import android.location.Address
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import xyz.malkki.neostumbler.extensions.getFromLocationSuspending
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import android.location.Geocoder as AndroidGeocoder

//Limit the amount of concurrent requests to avoid UI lag
private const val MAX_CONCURRENT_REQUESTS = 5

private const val MAX_FAILURES = 3

class PlatformGeocoder(private val geocoder: AndroidGeocoder, private val maxResults: Int = 1, private val returnEmptyIfNoGeocoder: Boolean = true): Geocoder {
    private val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

    //Count the number of IOExceptions to stop using the geocoder
    private val failureCount = AtomicInteger(0)

    override suspend fun getAddresses(latitude: Double, longitude: Double): List<Address> {
        if ((returnEmptyIfNoGeocoder && !AndroidGeocoder.isPresent()) || failureCount.get() >= MAX_FAILURES) {
            return emptyList()
        }

        return try {
            semaphore.withPermit {
                geocoder.getFromLocationSuspending(latitude, longitude, maxResults)
            }
        } catch (e: IOException) {
            failureCount.incrementAndGet()

            throw e
        }
    }
}