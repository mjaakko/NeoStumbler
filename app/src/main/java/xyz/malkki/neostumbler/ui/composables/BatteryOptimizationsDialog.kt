package xyz.malkki.neostumbler.ui.composables

import android.os.PowerManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.utils.OneTimeActionHelper
import xyz.malkki.neostumbler.utils.requestIgnoreBatteryOptimizations

private const val ACTION_NAME = "request_ignore_battery_optimizations"

@Composable
fun BatteryOptimizationsDialog(onBatteryOptimizationsDisabled: (Boolean) -> Unit) {
    val context = LocalContext.current

    val powerManager = context.getSystemService<PowerManager>()!!

    val oneTimeActionHelper = OneTimeActionHelper(context.applicationContext as StumblerApplication)

    val coroutineScope = rememberCoroutineScope()

    val batteryOptimizationActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { _ ->
            coroutineScope.launch {
                oneTimeActionHelper.markActionShown(ACTION_NAME)
            }

            //Check from PowerManager whether battery optimizations were disabled -> activity result code seems to be unreliable here
            val batteryOptimizationsDisabled = powerManager.isIgnoringBatteryOptimizations(context.packageName)

            onBatteryOptimizationsDisabled(batteryOptimizationsDisabled)
            if (!batteryOptimizationsDisabled) {
                Toast.makeText(context, context.getString(R.string.battery_optimizations_not_disabled_warning), Toast.LENGTH_SHORT).show()
            }
        }
    )

    if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
        //Already ignoring battery optimizations, no need to show dialog
        onBatteryOptimizationsDisabled(true)
        return
    }

    if (runBlocking { oneTimeActionHelper.hasActionBeenShown(ACTION_NAME) }) {
        //User has already been requested to disable battery optimizations
        onBatteryOptimizationsDisabled(false)
        return
    }

    AlertDialog(
        onDismissRequest = {
            //User dismissed the dialog -> do not show action again
            coroutineScope.launch {
                oneTimeActionHelper.markActionShown(ACTION_NAME)
            }
            onBatteryOptimizationsDisabled(false)
        },
        title = { Text(stringResource(id = R.string.ignore_battery_optimizations_title)) },
        text = { Text(stringResource(id = R.string.ignore_battery_optimizations_description)) },
        confirmButton = {
            TextButton(
                onClick = {
                    batteryOptimizationActivityLauncher.launch(requestIgnoreBatteryOptimizations(context.packageName))
                }) {
                Text(text = stringResource(R.string.ok))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(4.dp)
    )
}