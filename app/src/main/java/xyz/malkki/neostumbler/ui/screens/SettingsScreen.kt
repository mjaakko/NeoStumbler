package xyz.malkki.neostumbler.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.ui.composables.AutoScanToggle
import xyz.malkki.neostumbler.ui.composables.SettingsToggle

@Composable
fun SettingsScreen() {
    Column {
        SettingsToggle(title = stringResource(id = R.string.prefer_fused_location), preferenceKey = PreferenceKeys.PREFER_FUSED_LOCATION, default = true)
        AutoScanToggle()
    }
}
