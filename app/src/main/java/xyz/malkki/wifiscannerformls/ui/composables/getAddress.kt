package xyz.malkki.wifiscannerformls.ui.composables

import android.location.Address
import android.location.Geocoder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import timber.log.Timber
import xyz.malkki.wifiscannerformls.extensions.getFromLocationSuspending
import xyz.malkki.wifiscannerformls.extensions.roundToString
import java.io.IOException

@Composable
fun getAddress(latitude: Double, longitude: Double, geocoder: Geocoder): State<String> = produceState(
    initialValue = "",
    latitude,
    longitude,
    geocoder,
    producer = {
        val addresses = try {
            geocoder.getFromLocationSuspending(latitude, longitude, 1)
        } catch (ioException: IOException) {
            Timber.w(ioException, "Failed to geocode address for location ${latitude}, $longitude")
            emptyList()
        }

        value = if (addresses.isEmpty()) {
            "${latitude.roundToString(4)}, ${longitude.roundToString(4)}"
        } else {
            addresses.first().format()
        }
    }
)

private fun Address.format(): String {
    val firstAddressLine = getAddressLine(0)
    if (firstAddressLine != null) {
        return firstAddressLine
    }

    val streetAddress = if (subThoroughfare != null && thoroughfare != null) {
        "$thoroughfare $subThoroughfare"
    } else if (thoroughfare != null) {
        thoroughfare
    } else {
        null
    }

    val elements = listOfNotNull(streetAddress, locality, countryCode)
    return elements.joinToString(", ")
}
