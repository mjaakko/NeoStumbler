package xyz.malkki.neostumbler.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.scanner.ScannerService
import xyz.malkki.neostumbler.ui.composables.AboutNeoStumbler
import xyz.malkki.neostumbler.ui.composables.ReportReuploadButton
import xyz.malkki.neostumbler.ui.composables.settings.AutoScanToggle
import xyz.malkki.neostumbler.ui.composables.settings.AutoUploadToggle
import xyz.malkki.neostumbler.ui.composables.settings.CoverageLayerSettings
import xyz.malkki.neostumbler.ui.composables.settings.CrashLogSettingsItem
import xyz.malkki.neostumbler.ui.composables.settings.DbPruneSettings
import xyz.malkki.neostumbler.ui.composables.settings.FusedLocationToggle
import xyz.malkki.neostumbler.ui.composables.settings.IgnoreScanThrottlingToggle
import xyz.malkki.neostumbler.ui.composables.settings.LanguageSwitcher
import xyz.malkki.neostumbler.ui.composables.settings.ManageStorageSettingsItem
import xyz.malkki.neostumbler.ui.composables.settings.MovementDetectorSettings
import xyz.malkki.neostumbler.ui.composables.settings.PassiveScanToggle
import xyz.malkki.neostumbler.ui.composables.settings.ScannerNotificationStyleSettings
import xyz.malkki.neostumbler.ui.composables.settings.SettingsGroup
import xyz.malkki.neostumbler.ui.composables.settings.SettingsToggle
import xyz.malkki.neostumbler.ui.composables.settings.SliderSetting
import xyz.malkki.neostumbler.ui.composables.settings.geosubmit.GeosubmitEndpointSettings
import xyz.malkki.neostumbler.ui.composables.settings.privacy.WifiFilterSettings
import xyz.malkki.neostumbler.ui.composables.troubleshooting.TroubleshootingSettingsItem
import xyz.malkki.neostumbler.ui.modifiers.handleDisplayCutouts

@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    Column(
        modifier =
            Modifier.padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .handleDisplayCutouts()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        SettingsGroup(title = stringResource(id = R.string.settings_group_reports)) {
            GeosubmitEndpointSettings()
            CoverageLayerSettings()
            AutoUploadToggle()
            DbPruneSettings()
        }

        SettingsGroup(title = stringResource(id = R.string.settings_group_scanning)) {
            MovementDetectorSettings()
            FusedLocationToggle()
            IgnoreScanThrottlingToggle()

            SliderSetting(
                title = stringResource(R.string.pause_scanning_on_low_battery_title),
                preferenceKey = PreferenceKeys.PAUSE_ON_BATTERY_LEVEL_THRESHOLD,
                range = 0..50,
                step = 5,
                valueFormatter = {
                    if (it == 0) {
                        context.getString(R.string.disabled)
                    } else {
                        context.getString(R.string.pause_scanning_on_low_battery_description, it)
                    }
                },
                default = 0,
            )
            SliderSetting(
                title = stringResource(R.string.wifi_scan_frequency),
                preferenceKey = PreferenceKeys.WIFI_SCAN_DISTANCE,
                // Some translations assume this will always be a multiple of ten
                range = 10..250,
                step = 10,
                valueFormatter = {
                    ContextCompat.getString(context, R.string.every_x_meters).format(it)
                },
                default = ScannerService.DEFAULT_WIFI_SCAN_DISTANCE,
            )

            SliderSetting(
                title = stringResource(R.string.cell_tower_scan_frequency),
                preferenceKey = PreferenceKeys.CELL_SCAN_DISTANCE,
                // Some translations assume this will always be a multiple of ten
                range = 20..500,
                step = 20,
                valueFormatter = {
                    ContextCompat.getString(context, R.string.every_x_meters).format(it)
                },
                default = ScannerService.DEFAULT_CELL_SCAN_DISTANCE,
            )
            PassiveScanToggle()
            AutoScanToggle()
        }

        SettingsGroup(title = stringResource(id = R.string.settings_group_privacy)) {
            WifiFilterSettings()

            SettingsToggle(
                title = stringResource(id = R.string.reduced_metadata_title),
                description = stringResource(id = R.string.reduced_metadata_description),
                preferenceKey = PreferenceKeys.REDUCED_METADATA,
                default = false,
            )
        }

        SettingsGroup(title = stringResource(id = R.string.settings_group_other)) {
            ScannerNotificationStyleSettings()
            LanguageSwitcher()

            // Dynamic color is available on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsToggle(
                    title = stringResource(id = R.string.use_dynamic_color_ui),
                    preferenceKey = PreferenceKeys.DYNAMIC_COLOR_THEME,
                    default = false,
                )
            }

            TroubleshootingSettingsItem()
            ManageStorageSettingsItem()

            CrashLogSettingsItem()
        }

        Spacer(modifier = Modifier.height(8.dp))

        ReportReuploadButton()

        Spacer(modifier = Modifier.height(8.dp))

        AboutNeoStumbler()

        Spacer(modifier = Modifier.height(16.dp))
    }
}
