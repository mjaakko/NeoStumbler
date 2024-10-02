package xyz.malkki.neostumbler.ui.composables.troubleshooting

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.MutableStateFlow
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.checkMissingPermissions

//All permissions needed for optimal scanning results
private val SCAN_PERMISSIONS = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.READ_PHONE_STATE)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
    } else {
        add(Manifest.permission.BLUETOOTH)
        add(Manifest.permission.BLUETOOTH_ADMIN)
    }
}.toTypedArray()

private fun Context.allPermissionsGranted(): Boolean {
    return checkMissingPermissions(*SCAN_PERMISSIONS).isEmpty()
}

@Composable
fun PermissionsTroubleshootingItem() {
    val context = LocalContext.current

    val permissionsState = MutableStateFlow(context.allPermissionsGranted())
    val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionsState.tryEmit(context.allPermissionsGranted())
    }

    TroubleshootingItem(
        title = stringResource(id = R.string.troubleshooting_permissions_granted),
        stateFlow = permissionsState,
        fixAction = {
            permissionsLauncher.launch(SCAN_PERMISSIONS)
        }
    )
}