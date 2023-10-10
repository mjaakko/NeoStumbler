package xyz.malkki.wifiscannerformls.ui.composables

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import xyz.malkki.wifiscannerformls.R
import xyz.malkki.wifiscannerformls.extensions.getActivity

@Composable
fun PermissionsDialog(missingPermissions: List<String>, permissionRationales: Map<String, String>, onPermissionsGranted: (Map<String, Boolean>) -> Unit) {
    if (missingPermissions.isEmpty()) {
        return
    }

    val context = LocalContext.current

    val permissionsLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        onPermissionsGranted(permissions)
    }

    val permissionRationaleText = missingPermissions
        .mapNotNull {
            if (ActivityCompat.shouldShowRequestPermissionRationale(context.getActivity()!!, it)) {
                permissionRationales[it]
            } else {
                null
            }
        }
        .distinct()
        .joinToString("\n\n")

    if (permissionRationaleText.isNotBlank()) {
        AlertDialog(
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            onDismissRequest = {},
            title = { Text(text = stringResource(R.string.permissions_needed)) },
            text = {
                Text(permissionRationaleText)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionsLauncher.launch(missingPermissions.toTypedArray())
                    }) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(4.dp)
        )
    } else {
        SideEffect {
            permissionsLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}