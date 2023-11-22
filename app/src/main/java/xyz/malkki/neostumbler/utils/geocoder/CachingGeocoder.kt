package xyz.malkki.neostumbler.utils.geocoder

import android.location.Address
import androidx.collection.LruCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap

private typealias LocationKey = Pair<BigDecimal, BigDecimal>

class CachingGeocoder(private val actualGeocoder: Geocoder): Geocoder {
    private val cache = LruCache<LocationKey, List<Address>>(100)

    private val ongoingRequests = ConcurrentHashMap<LocationKey, Deferred<List<Address>>>()

    override suspend fun getAddresses(latitude: Double, longitude: Double): List<Address> = coroutineScope {
        val key = latitude.roundCoordinate() to longitude.roundCoordinate()

        val cachedAddresses = cache[key]
        if (cachedAddresses != null) {
            return@coroutineScope cachedAddresses
        }

        val futureAddresses = ongoingRequests.compute(key) { _, existing ->
            existing ?: async(start = CoroutineStart.LAZY) {
                val addresses = actualGeocoder.getAddresses(key.first.toDouble(), key.second.toDouble())

                cache.put(key, addresses)
                ongoingRequests.remove(key)

                addresses
            }
        }!!

        futureAddresses.await()
    }

    private fun Double.roundCoordinate(): BigDecimal = toBigDecimal().setScale(5, RoundingMode.HALF_UP)
}