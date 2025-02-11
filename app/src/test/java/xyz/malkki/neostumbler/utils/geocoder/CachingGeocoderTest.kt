package xyz.malkki.neostumbler.utils.geocoder

import android.location.Address
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class CachingGeocoderTest {
    @Test
    fun `Test geocoding results are cached`() {
        val fakeGeocoder =
            object : Geocoder {
                override suspend fun getAddresses(
                    latitude: Double,
                    longitude: Double,
                ): List<Address> {
                    delay(5.seconds)
                    return listOf(Address(Locale.ROOT))
                }
            }

        val cachingGeocoder = CachingGeocoder(fakeGeocoder)

        val durationInitial = measureTime {
            runBlocking { cachingGeocoder.getAddresses(4.324, 6.7474) }
        }

        assertTrue(durationInitial >= 5.seconds)

        val duration = measureTime { runBlocking { cachingGeocoder.getAddresses(4.324, 6.7474) } }

        // Subsequent geocoder queries should be faster because they use cached results
        assertTrue(duration <= durationInitial)
    }
}
