package xyz.malkki.neostumbler.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
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
import xyz.malkki.neostumbler.ui.composables.AboutNeoStumbler
import xyz.malkki.neostumbler.ui.composables.ExportDataButton
import xyz.malkki.neostumbler.ui.composables.ReportReuploadButton
import xyz.malkki.neostumbler.ui.composables.SettingsGroup
import xyz.malkki.neostumbler.ui.composables.SettingsToggle
import xyz.malkki.neostumbler.ui.composables.autoscan.AutoScanToggle
import xyz.malkki.neostumbler.ui.composables.settings.AutoUploadToggle
import xyz.malkki.neostumbler.ui.composables.settings.DbPruneSettings
import xyz.malkki.neostumbler.ui.composables.settings.IgnoreScanThrottlingToggle
import xyz.malkki.neostumbler.ui.composables.settings.LanguageSwitcher
import xyz.malkki.neostumbler.ui.composables.settings.ManageStorageSettingsItem
import xyz.malkki.neostumbler.ui.composables.settings.MovementDetectorSettings
import xyz.malkki.neostumbler.ui.composables.settings.ScannerNotificationStyleSettings
import xyz.malkki.neostumbler.ui.composables.settings.geosubmit.GeosubmitEndpointSettings

@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SettingsGroup(
            title = stringResource(id = R.string.settings_group_reports)
        ) {
            GeosubmitEndpointSettings()
            AutoUploadToggle()
            DbPruneSettings()
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

            // Dynamic color is available on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsToggle(
                    title = stringResource(id = R.string.use_dynamic_color_ui),
                    preferenceKey = PreferenceKeys.DYNAMIC_COLOR_THEME,
                    default = false
                )
            }

            ManageStorageSettingsItem()
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ExportDataButton()
            ReportReuploadButton()
        }

        Spacer(modifier = Modifier.height(8.dp))

        AboutNeoStumbler()
    }
}
