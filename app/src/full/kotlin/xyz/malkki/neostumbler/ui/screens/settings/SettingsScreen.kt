package xyz.malkki.neostumbler.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.ui.composables.ExportDataButton
import xyz.malkki.neostumbler.ui.composables.ReportReuploadButton
import xyz.malkki.neostumbler.ui.composables.SettingsGroup
import xyz.malkki.neostumbler.ui.composables.SettingsToggle
import xyz.malkki.neostumbler.ui.composables.autoscan.AutoScanToggle
import xyz.malkki.neostumbler.ui.composables.settings.AutoUploadToggle
import xyz.malkki.neostumbler.ui.composables.settings.geosubmit.GeosubmitEndpointSettings
import xyz.malkki.neostumbler.ui.composables.settings.IgnoreScanThrottlingToggle
import xyz.malkki.neostumbler.ui.composables.settings.LanguageSwitcher
import xyz.malkki.neostumbler.ui.composables.settings.MovementDetectorSettings
import xyz.malkki.neostumbler.ui.composables.settings.ScannerNotificationStyleSettings

@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SettingsGroup(
            title = stringResource(id = R.string.settings_group_reports)
        ) {
            GeosubmitEndpointSettings()
            AutoUploadToggle()
        }

        SettingsGroup(
            title = stringResource(id = R.string.settings_group_scanning)
        ) {
            MovementDetectorSettings()
            SettingsToggle(title = stringResource(id = R.string.prefer_fused_location), preferenceKey = PreferenceKeys.PREFER_FUSED_LOCATION, default = true)
            IgnoreScanThrottlingToggle()
            AutoScanToggle()
        }

        SettingsGroup(title = stringResource(id = R.string.settings_group_other)) {
            ScannerNotificationStyleSettings()
            LanguageSwitcher()
        }

        Spacer(modifier = Modifier.height(20.dp))
        ExportDataButton()
        Spacer(modifier = Modifier.height(8.dp))
        ReportReuploadButton()
    }
}
