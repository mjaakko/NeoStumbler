package xyz.malkki.neostumbler.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.ui.composables.troubleshooting.AccurateLocationTroubleshootingItem
import xyz.malkki.neostumbler.ui.composables.troubleshooting.BatteryOptimizationTroubleshootingItem
import xyz.malkki.neostumbler.ui.composables.troubleshooting.PermissionsTroubleshootingItem
import xyz.malkki.neostumbler.ui.composables.troubleshooting.WifiScanAlwaysAvailableTroubleshootingItem

@Composable
fun TroubleshootingView() {
    val showDialog = remember {
        mutableStateOf(false)
    }

    if (showDialog.value) {
        BasicAlertDialog(
            onDismissRequest = {
                showDialog.value = false
            }
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(all = 16.dp)
                ) {
                    Text(
                        style = MaterialTheme.typography.titleLarge,
                        text = stringResource(id = R.string.troubleshooting)
                    )

                    Spacer(modifier = Modifier.height(height = 8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        PermissionsTroubleshootingItem()

                        AccurateLocationTroubleshootingItem()

                        BatteryOptimizationTroubleshootingItem()

                        WifiScanAlwaysAvailableTroubleshootingItem()
                    }
                }
            }
        }
    }

    Button(
        onClick = {
            showDialog.value = true
        }
    ) {
        Text(
            text = stringResource(id = R.string.troubleshooting)
        )
    }
}