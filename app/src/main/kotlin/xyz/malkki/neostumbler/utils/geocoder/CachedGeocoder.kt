package xyz.malkki.neostumbler.utils.geocoder

import androidx.collection.SieveCache
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.geohex.geohex4j.GeoHex
import timber.log.Timber
import xyz.malkki.neostumbler.data.geocoder.Geocoder
import xyz.malkki.neostumbler.geography.LatLng

private typealias CacheKey = Pair<String, Locale>

private const val CACHE_MAX_SIZE = 100

/**
 * Geohex is used as the cache key. Coordinates within the same Geohex will return the cached
 * address
 */
private const val GEOHEX_RESOLUTION = 10

/**
 * Devices without a geocoder (or with the geocoder disabled) never return a value -> use a timeout
 * to avoid waiting indefinitely
 *
 * There is no reliable way to check whether a geocoder available other than to try using it
 */
private val GEOCODE_TIMEOUT = 20.seconds

class CachedGeocoder(private val coroutineScope: CoroutineScope, private val geocoder: Geocoder) :
    Geocoder {
    private val mutex = Mutex()

    private val cache =
        SieveCache<CacheKey, Deferred<String?>>(
            maxSize = CACHE_MAX_SIZE,
            createValueFromKey = { (geohexCode, locale) ->
                val zone = GeoHex.getZoneByCode(geohexCode)

                coroutineScope.async {
                    withTimeoutOrNull(GEOCODE_TIMEOUT) {
                        @Suppress("TooGenericExceptionCaught")
                        try {
                            geocoder.getAddress(locale, LatLng(zone.lat, zone.lon))
                        } catch (ex: Exception) {
                            Timber.w(
                                ex,
                                "Failed to geocode address for location ${zone.lat}, ${zone.lon}",
                            )

                            null
                        }
                    }
                }
            },
        )

    override suspend fun getAddress(locale: Locale, latLng: LatLng): String? {
        val geohexCode = GeoHex.encode(latLng.latitude, latLng.longitude, GEOHEX_RESOLUTION)

        val deferredAddress = mutex.withLock { cache[geohexCode to locale] }
        return deferredAddress?.await()
    }
}
