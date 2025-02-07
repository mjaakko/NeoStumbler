package xyz.malkki.neostumbler.ui.composables.troubleshooting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.ui.composables.settings.SettingsItem

@Composable
fun TroubleshootingSettingsItem() {
    val showDialog = rememberSaveable { mutableStateOf(false) }

    if (showDialog.value) {
        BasicAlertDialog(onDismissRequest = { showDialog.value = false }) {
            Surface(
                modifier = Modifier.sizeIn(maxWidth = 400.dp).fillMaxWidth().wrapContentHeight(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(modifier = Modifier.padding(all = 24.dp)) {
                    Text(
                        style = MaterialTheme.typography.titleLarge,
                        text = stringResource(id = R.string.troubleshooting_title),
                    )

                    Column(
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                    ) {
                        PermissionsTroubleshootingItem()

                        AccurateLocationTroubleshootingItem()

                        BatteryOptimizationTroubleshootingItem()

                        WifiScanAlwaysAvailableTroubleshootingItem()
                    }

                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = { showDialog.value = false },
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            }
        }
    }

    SettingsItem(
        title = stringResource(R.string.troubleshooting_title),
        description = stringResource(R.string.troubleshooting_description),
        onClick = { showDialog.value = true },
    )
}
