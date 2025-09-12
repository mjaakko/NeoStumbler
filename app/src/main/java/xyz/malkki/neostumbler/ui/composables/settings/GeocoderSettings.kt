package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getEnumFlow
import xyz.malkki.neostumbler.data.settings.setEnum
import xyz.malkki.neostumbler.utils.geocoder.GeocoderType

@Composable
fun GeocoderSettings(settings: Settings = koinInject()) {
    val context = LocalContext.current

    val selectedGeocoder by
        settings
            .getEnumFlow(PreferenceKeys.GEOCODER_TYPE, GeocoderType.DEFAULT)
            .collectAsStateWithLifecycle(initialValue = null)

    if (selectedGeocoder != null) {
        MultiChoiceSettings(
            title = stringResource(id = R.string.geocoder_settings_title),
            options = GeocoderType.entries,
            selectedOption = selectedGeocoder!!,
            titleProvider = { geocoderType -> context.getString(geocoderType.titleResId) },
            onValueSelected = { newGeocoderType ->
                settings.edit { setEnum(PreferenceKeys.GEOCODER_TYPE, newGeocoderType) }
            },
        )
    }
}
