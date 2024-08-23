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
import xyz.malkki.neostumbler.ui.composables.ExportDataButton
import xyz.malkki.neostumbler.ui.composables.ReportReuploadButton
import xyz.malkki.neostumbler.ui.composables.SettingsGroup
import xyz.malkki.neostumbler.ui.composables.settings.AutoUploadToggle
import xyz.malkki.neostumbler.ui.composables.settings.GeosubmitEndpointSettings
import xyz.malkki.neostumbler.ui.composables.settings.IgnoreScanThrottlingToggle
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
            IgnoreScanThrottlingToggle()
        }

        SettingsGroup(title = stringResource(id = R.string.settings_group_other)) {
            ScannerNotificationStyleSettings()
        }

        Spacer(modifier = Modifier.height(20.dp))
        ExportDataButton()
        Spacer(modifier = Modifier.height(8.dp))
        ReportReuploadButton()
    }
}
