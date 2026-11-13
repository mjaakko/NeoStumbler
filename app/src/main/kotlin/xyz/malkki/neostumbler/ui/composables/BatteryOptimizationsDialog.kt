package xyz.malkki.neostumbler.ui.composables

import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.utils.OneTimeActionHelper
import xyz.malkki.neostumbler.utils.requestIgnoreBatteryOptimizations

private const val ACTION_NAME = "request_ignore_battery_optimizations"

@Composable
fun BatteryOptimizationsDialog(
    oneTimeActionHelper: OneTimeActionHelper = koinInject(),
    onCloseDialog: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val powerManager: PowerManager? = remember(context) { context.getSystemService() }

    val batteryOptimizationActivityLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = { _ ->
                // Check from PowerManager whether battery optimizations were disabled -> activity
                // result code seems to be unreliable here
                val batteryOptimizationsDisabled =
                    powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

                if (!batteryOptimizationsDisabled) {
                    context.showToast(
                        ContextCompat.getString(
                            context,
                            R.string.battery_optimizations_not_disabled_warning,
                        )
                    )
                }

                coroutineScope.launch {
                    oneTimeActionHelper.markActionShown(ACTION_NAME)

                    onCloseDialog()
                }
            },
        )

    if (powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true) {
        // Already ignoring battery optimizations, no need to show dialog
        SideEffect { onCloseDialog() }
        return
    }

    val dialogAlreadyShown by
        oneTimeActionHelper
            .hasActionBeenShownFlow(ACTION_NAME)
            .collectAsStateWithLifecycle(initialValue = null)

    when (dialogAlreadyShown) {
        true -> {
            SideEffect {
                // User has already been requested to disable battery optimizations
                onCloseDialog()
            }
        }
        false -> {
            AlertDialog(
                onDismissRequest = {
                    // User dismissed the dialog -> do not show action again
                    coroutineScope.launch {
                        oneTimeActionHelper.markActionShown(ACTION_NAME)

                        onCloseDialog()
                    }
                },
                title = { Text(stringResource(id = R.string.ignore_battery_optimizations_title)) },
                text = {
                    Text(stringResource(id = R.string.ignore_battery_optimizations_description))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            batteryOptimizationActivityLauncher.launch(
                                requestIgnoreBatteryOptimizations(context.packageName)
                            )
                        }
                    ) {
                        Text(text = stringResource(R.string.ok))
                    }
                },
            )
        }
        null -> {}
    }
}
