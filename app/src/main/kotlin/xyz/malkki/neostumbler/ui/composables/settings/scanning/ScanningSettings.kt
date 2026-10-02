package xyz.malkki.neostumbler.ui.composables.settings.scanning

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.activescan.ActiveScanDefaults
import xyz.malkki.neostumbler.activescan.ActiveScanPreferenceKeys
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.ui.composables.settings.AutoScanToggle
import xyz.malkki.neostumbler.ui.composables.settings.FusedLocationToggle
import xyz.malkki.neostumbler.ui.composables.settings.IgnoreScanThrottlingToggle
import xyz.malkki.neostumbler.ui.composables.settings.SettingsGroup
import xyz.malkki.neostumbler.ui.composables.settings.SettingsToggle
import xyz.malkki.neostumbler.ui.composables.settings.SliderSetting

@Composable
fun ScanningSettings() {
    SettingsGroup(title = stringResource(id = R.string.settings_group_scanning)) {
        MovementDetectorSettings()
        FusedLocationToggle()
        IgnoreScanThrottlingToggle()

        SliderSetting(
            title = stringResource(R.string.pause_scanning_on_low_battery_title),
            preferenceKey = ActiveScanPreferenceKeys.PAUSE_ON_BATTERY_LEVEL_THRESHOLD,
            range = 0..50,
            step = 5,
            valueFormatter = {
                if (it == 0) {
                    stringResource(R.string.disabled)
                } else {
                    stringResource(R.string.pause_scanning_on_low_battery_description, it)
                }
            },
            default = 0,
        )

        SettingsToggle(
            title = stringResource(id = R.string.pause_scanning_when_overheating),
            preferenceKey = ActiveScanPreferenceKeys.PAUSE_WHEN_OVERHEATING,
            default = false,
        )

        SliderSetting(
            title = stringResource(R.string.wifi_scan_frequency),
            preferenceKey = ActiveScanPreferenceKeys.WIFI_SCAN_DISTANCE,
            // Some translations assume this will always be a multiple of ten
            range = 10..250,
            step = 10,
            valueFormatter = { stringResource(R.string.every_x_meters, it) },
            default = ActiveScanDefaults.WIFI_SCAN_DISTANCE,
        )

        SliderSetting(
            title = stringResource(R.string.cell_tower_scan_frequency),
            preferenceKey = ActiveScanPreferenceKeys.CELL_SCAN_DISTANCE,
            // Some translations assume this will always be a multiple of ten
            range = 20..500,
            step = 20,
            valueFormatter = { stringResource(R.string.every_x_meters, it) },
            default = ActiveScanDefaults.CELL_SCAN_DISTANCE,
        )

        WifiRttRangingSettings()

        SettingsToggle(
            title = stringResource(id = R.string.moving_device_filter_title),
            description = stringResource(id = R.string.moving_device_filter_description),
            preferenceKey = PreferenceKeys.FILTER_MOVING_DEVICES,
            default = true,
        )

        PassiveScanToggle()
        AutoScanToggle()
    }
}
