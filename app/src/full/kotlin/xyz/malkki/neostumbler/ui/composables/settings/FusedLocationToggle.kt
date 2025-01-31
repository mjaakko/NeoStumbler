package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.ui.res.stringResource
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.ui.composables.SettingsToggle

@androidx.compose.runtime.Composable
fun FusedLocationToggle() {
    SettingsToggle(
        title = stringResource(id = R.string.prefer_fused_location_title),
        description = stringResource(id = R.string.prefer_fused_location_description),
        preferenceKey = PreferenceKeys.PREFER_FUSED_LOCATION,
        default = true
    )
}
