package xyz.malkki.neostumbler.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.utils.OneTimeActionHelper

private const val MLS_WARNING = "mls_warning"

@Composable
fun MLSWarningDialog() {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val oneTimeActionHelper = OneTimeActionHelper(context.applicationContext as StumblerApplication)

    val warningShown = oneTimeActionHelper.hasActionBeenShownFlow(MLS_WARNING).collectAsState(
        initial = true
    )

    if (!warningShown.value) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(id = R.string.mls_warning_title)) },
            text = { Text(stringResource(id = R.string.mls_warning_text)) },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        oneTimeActionHelper.markActionShown(MLS_WARNING)
                    }
                }) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }
}