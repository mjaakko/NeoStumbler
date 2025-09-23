package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import java.util.Locale
import xyz.malkki.neostumbler.data.geocoder.Geocoder
import xyz.malkki.neostumbler.extensions.roundToString
import xyz.malkki.neostumbler.geography.LatLng

private const val FRACTION_DIGITS = 4

@Composable
fun getAddress(latLng: LatLng, locale: Locale, geocoder: Geocoder): State<String?> =
    produceState(
        initialValue = null,
        latLng,
        locale,
        geocoder,
        producer = {
            val address =
                try {
                    geocoder.getAddress(locale, latLng)
                } catch (_: Exception) {
                    null
                }

            value =
                if (address != null) {
                    address
                } else {
                    val latFormatted = latLng.latitude.roundToString(FRACTION_DIGITS)
                    val lngFormatted = latLng.longitude.roundToString(FRACTION_DIGITS)

                    "${latFormatted}, $lngFormatted"
                }
        },
    )
