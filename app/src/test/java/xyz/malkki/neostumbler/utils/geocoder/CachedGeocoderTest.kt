package xyz.malkki.neostumbler.utils.geocoder

import java.util.Locale
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import xyz.malkki.neostumbler.data.geocoder.Geocoder
import xyz.malkki.neostumbler.geography.LatLng

class CachedGeocoderTest {
    @Test
    fun `Cached geocoder returns cached results for the same coordinate`() = runTest {
        val mockGeocoder =
            mock<Geocoder> { onBlocking { getAddress(any(), any()) } doReturn "street 123" }

        val cachedGeocoder = CachedGeocoder(backgroundScope, mockGeocoder)

        Assert.assertEquals(
            "street 123",
            cachedGeocoder.getAddress(Locale.US, LatLng(56.543254, 12.431565)),
        )
        Assert.assertEquals(
            "street 123",
            cachedGeocoder.getAddress(Locale.US, LatLng(56.543254, 12.431565)),
        )

        verify(mockGeocoder, times(1)).getAddress(any(), any())
    }

    @Test
    fun `Cached geocoder does not wait for results indefinitely`() = runTest {
        val mockGeocoder =
            mock<Geocoder> {
                onBlocking { getAddress(any(), any()) } doSuspendableAnswer
                    { invocation ->
                        delay(10.days)

                        "street 123"
                    }
            }

        val cachedGeocoder = CachedGeocoder(backgroundScope, mockGeocoder)

        val (address, elapsedTime) =
            testTimeSource.measureTimedValue {
                cachedGeocoder.getAddress(Locale.CHINESE, LatLng(10.1451516, -124.41566))
            }

        assertNull(address)
        assertTrue(elapsedTime >= 20.seconds)
    }
}
