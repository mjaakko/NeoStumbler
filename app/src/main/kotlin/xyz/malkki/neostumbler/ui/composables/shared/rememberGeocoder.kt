package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.geocoder.Geocoder
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getEnumFlow
import xyz.malkki.neostumbler.utils.geocoder.CachedGeocoder
import xyz.malkki.neostumbler.utils.geocoder.GeocoderType
import xyz.malkki.neostumbler.utils.geocoder.StubGeocoder

@Composable
fun rememberGeocoder(
    geocoder: Geocoder = koinInject(),
    settings: Settings = koinInject(),
): Geocoder {
    val coroutineScope = rememberCoroutineScope()

    val geocoderType by
        settings
            .getEnumFlow(PreferenceKeys.GEOCODER_TYPE, GeocoderType.DEFAULT)
            .collectAsStateWithLifecycle(initialValue = null)

    return remember(geocoderType, geocoder, coroutineScope) {
        if (geocoderType == GeocoderType.DEFAULT) {
            CachedGeocoder(coroutineScope, geocoder)
        } else {
            StubGeocoder
        }
    }
}
