package xyz.malkki.neostumbler.ui.composables.troubleshooting

import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.utils.requestIgnoreBatteryOptimizations

@Composable
fun BatteryOptimizationTroubleshootingItem() {
    val context = LocalContext.current

    val powerManager: PowerManager? = remember(context) { context.getSystemService() }

    var batteryOptimizationsDisabled by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
    }
    val batteryOptimizationsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            batteryOptimizationsDisabled =
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        }

    TroubleshootingItem(
        title = stringResource(id = R.string.troubleshooting_battery_optimizations_disabled),
        isOk = batteryOptimizationsDisabled,
        fixAction = {
            batteryOptimizationsLauncher.launch(
                requestIgnoreBatteryOptimizations(context.packageName)
            )
        },
    )
}
