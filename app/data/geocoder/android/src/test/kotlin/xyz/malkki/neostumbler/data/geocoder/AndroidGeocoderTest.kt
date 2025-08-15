package xyz.malkki.neostumbler.data.geocoder

import android.content.Context
import java.io.IOException
import java.util.Locale
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import xyz.malkki.neostumbler.geography.LatLng

class AndroidGeocoderTest {
    @Test
    fun `Geocoder gets disabled if it fails too many times`() = runTest {
        val mockContext = mock<Context>()
        val mockGeocoder =
            mock<android.location.Geocoder> {
                @Suppress("DEPRECATION")
                on { getFromLocation(any(), any(), any()) } doThrow IOException("IO exception")
            }

        val geocoder =
            AndroidGeocoder(
                mockContext,
                returnNullIfNoGeocoder = false,
                timeSource = testTimeSource,
                geocoderFactory = { _, _ -> mockGeocoder },
            )

        repeat(4) {
            try {
                geocoder.getAddress(Locale.getDefault(), LatLng(45.14315, 78.765748))
            } catch (_: Exception) {}
        }

        // Underlying geocoder should be only used until it has thrown 3 exceptions
        @Suppress("DEPRECATION") verify(mockGeocoder, times(3)).getFromLocation(any(), any(), any())
        reset(mockGeocoder)

        delay(10.minutes + 1.seconds)

        try {
            geocoder.getAddress(Locale.getDefault(), LatLng(45.14315, 78.765748))
        } catch (_: Exception) {}

        // After 10 minutes, the geocoder should be enabled again
        @Suppress("DEPRECATION") verify(mockGeocoder, times(1)).getFromLocation(any(), any(), any())
    }
}
