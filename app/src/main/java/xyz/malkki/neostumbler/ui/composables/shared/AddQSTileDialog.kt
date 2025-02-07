package xyz.malkki.neostumbler.ui.composables.shared

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.utils.ImmediateExecutor

/**
 * Shows a dialog which prompts to add a quick settings tile
 *
 * @param componentName Component name of the [android.service.quicksettings.TileService]
 * @param dialogText Text to show in the dialog
 * @param onDialogDismissed Callback for when the dialog is closed
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AddQSTileDialog(componentName: ComponentName, dialogText: String, onDialogDismissed: () -> Unit) {
    val context = LocalContext.current

    val packageManager = context.packageManager
    val statusBarManager = context.getSystemService<StatusBarManager>()!!

    val serviceInfo = packageManager.getServiceInfo(componentName, 0)

    AlertDialog(
        onDismissRequest = onDialogDismissed,
        confirmButton = {
            TextButton(
                onClick = {
                    statusBarManager.requestAddTileService(
                        componentName,
                        serviceInfo.loadLabel(packageManager),
                        Icon.createWithResource(context, serviceInfo.icon),
                        ImmediateExecutor
                    ) {
                        onDialogDismissed()
                    }
                }
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDialogDismissed
            ) {
                Text(stringResource(id = R.string.no_thanks))
            }
        },
        text = {
            Text(text = dialogText)
        }
    )
}