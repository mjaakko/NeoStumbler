package xyz.malkki.neostumbler.utils.geocoder

import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
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
}
