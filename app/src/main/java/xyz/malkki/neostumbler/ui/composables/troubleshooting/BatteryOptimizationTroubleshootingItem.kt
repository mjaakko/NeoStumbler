package xyz.malkki.neostumbler.ui.composables.troubleshooting

import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.utils.requestIgnoreBatteryOptimizations

@Composable
fun BatteryOptimizationTroubleshootingItem() {
    val context = LocalContext.current

    val powerManager = context.getSystemService<PowerManager>()!!

    val batteryOptimizationsDisabled = MutableStateFlow(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    val batteryOptimizationsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        batteryOptimizationsDisabled.tryEmit(powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }

    TroubleshootingItem(
        title = stringResource(id = R.string.troubleshooting_battery_optimizations_disabled),
        stateFlow = batteryOptimizationsDisabled,
        fixAction = {
            batteryOptimizationsLauncher.launch(requestIgnoreBatteryOptimizations(context.packageName))
        }
    )
}