package xyz.malkki.neostumbler.utils.geocoder

import android.location.Address
import androidx.collection.LruCache
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.geohex.geohex4j.GeoHex
import java.util.concurrent.ConcurrentHashMap

class CachingGeocoder(private val actualGeocoder: Geocoder): Geocoder {
    private val cache = LruCache<String, List<Address>>(100)

    private val ongoingRequests = ConcurrentHashMap<String, Deferred<List<Address>>>()

    override suspend fun getAddresses(latitude: Double, longitude: Double): List<Address> = coroutineScope {
        val key = GeoHex.encode(latitude, longitude, 10)

        val cachedAddresses = cache[key]
        if (cachedAddresses != null) {
            return@coroutineScope cachedAddresses
        }

        val futureAddresses = ongoingRequests.compute(key) { _, existing ->
            existing ?: async(start = CoroutineStart.LAZY) {
                val zone = GeoHex.getZoneByCode(key)

                val addresses = actualGeocoder.getAddresses(zone.lat, zone.lon)

                cache.put(key, addresses)
                ongoingRequests.remove(key)

                addresses
            }
        }!!

        futureAddresses.await()
    }
}