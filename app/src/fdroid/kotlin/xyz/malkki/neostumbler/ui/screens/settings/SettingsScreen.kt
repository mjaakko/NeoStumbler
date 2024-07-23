package xyz.malkki.neostumbler.ui.screens.settings


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.malkki.neostumbler.ui.composables.ExportDataButton
import xyz.malkki.neostumbler.ui.composables.ReportReuploadButton
import xyz.malkki.neostumbler.ui.composables.settings.AutoUploadToggle
import xyz.malkki.neostumbler.ui.composables.settings.GeosubmitEndpointSettings
import xyz.malkki.neostumbler.ui.composables.settings.IgnoreScanThrottlingToggle

@Composable
fun SettingsScreen() {
    Column {
        GeosubmitEndpointSettings()
        AutoUploadToggle()
        IgnoreScanThrottlingToggle()
        Spacer(modifier = Modifier.height(20.dp))
        ExportDataButton()
        Spacer(modifier = Modifier.height(8.dp))
        ReportReuploadButton()
    }
}
