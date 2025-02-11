package xyz.malkki.neostumbler.utils.geocoder

import android.location.Geocoder
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class PlatformGeocoderTest {
    @Test
    fun `Test geocoder disables itself after 3 IOExceptions`(): Unit = runBlocking {
        val mockGeocoder =
            mock<Geocoder> {
                on { getFromLocation(any(), any(), any()) } doThrow IOException("IO exception")
            }

        val geocoder = PlatformGeocoder(mockGeocoder, returnEmptyIfNoGeocoder = false)

        repeat(4) {
            try {
                geocoder.getAddresses(65.7, 75.7)
            } catch (_: Exception) {}
        }

        // Underlying geocoder should be only used until it has thrown 3 exceptions
        verify(mockGeocoder, times(3)).getFromLocation(any(), any(), any())
    }
}
