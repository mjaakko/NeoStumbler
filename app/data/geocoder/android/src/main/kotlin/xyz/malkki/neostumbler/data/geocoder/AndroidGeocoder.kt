package xyz.malkki.neostumbler.data.geocoder

import android.content.Context
import android.location.Address
import android.os.SystemClock
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.AbstractLongTimeSource
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import xyz.malkki.neostumbler.data.geocoder.extensions.getFromLocationSuspending
import xyz.malkki.neostumbler.geography.LatLng

// Limit the amount of concurrent requests to avoid UI lag
private const val MAX_CONCURRENT_REQUESTS = 5

// If geocoding fails more than 3 times with an IOException, there's no geocoder
// implementation available or there's no network connection. In either case, disable the geocoder
// temporarily
private const val MAX_FAILURES = 3

private val RETRY_AFTER = 10.minutes

/**
 * @param returnNullIfNoGeocoder For testing
 * @param timeSource For testing
 * @param geocoderFactory For testing
 */
class AndroidGeocoder(
    context: Context,
    private val returnNullIfNoGeocoder: Boolean = true,
    private val timeSource: TimeSource =
        object : AbstractLongTimeSource(DurationUnit.MILLISECONDS) {
            override fun read(): Long {
                return SystemClock.elapsedRealtime()
            }
        },
    geocoderFactory: (Context, Locale) -> android.location.Geocoder = { context, locale ->
        android.location.Geocoder(context, locale)
    },
) : Geocoder {
    private val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

    private val geocoderHolder = GeocoderHolder(context, Locale.getDefault(), geocoderFactory)

    private val failureCount = AtomicInteger(0)
    private var lastFailure: TimeMark = timeSource.markNow()

    override suspend fun getAddress(locale: Locale, latLng: LatLng): String? {
        if (returnNullIfNoGeocoder && !android.location.Geocoder.isPresent()) {
            return null
        }

        if (failureCount.get() >= MAX_FAILURES && lastFailure.elapsedNow() <= RETRY_AFTER) {
            return null
        }

        val geocoder = geocoderHolder.getGeocoder(locale)

        val address =
            try {
                semaphore.withPermit {
                    geocoder.getFromLocationSuspending(latLng.latitude, latLng.longitude, 1)
                }
            } catch (ioe: IOException) {
                failureCount.incrementAndGet()
                lastFailure = timeSource.markNow()

                throw ioe
            }

        return address.firstOrNull()?.format()
    }

    private class GeocoderHolder(
        private val context: Context,
        private var locale: Locale,
        private val geocoderFactory: (Context, Locale) -> android.location.Geocoder,
    ) {
        private val mutex = Mutex()

        private var geocoder = geocoderFactory(context, locale)

        suspend fun getGeocoder(locale: Locale): android.location.Geocoder =
            mutex.withLock {
                return if (locale == this.locale) {
                    geocoder
                } else {
                    this.locale = locale
                    this.geocoder = geocoderFactory(context, this.locale)

                    this.geocoder
                }
            }
    }
}

private fun Address.format(): String {
    val firstAddressLine = getAddressLine(0)
    if (firstAddressLine != null) {
        return firstAddressLine
    }

    val streetAddress =
        if (subThoroughfare != null && thoroughfare != null) {
            "$thoroughfare $subThoroughfare"
        } else if (thoroughfare != null) {
            thoroughfare
        } else {
            null
        }

    val elements = listOfNotNull(streetAddress, locality, countryCode)
    return elements.joinToString(", ")
}
