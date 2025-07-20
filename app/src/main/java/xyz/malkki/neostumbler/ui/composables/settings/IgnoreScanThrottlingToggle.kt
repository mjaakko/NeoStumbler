package xyz.malkki.neostumbler.ui.composables.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.extensions.isWifiScanThrottled
import xyz.malkki.neostumbler.ui.composables.ToggleWithAction

@Composable
fun IgnoreScanThrottlingToggle(settings: Settings = koinInject()) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val enabled =
        settings
            .getBooleanFlow(PreferenceKeys.IGNORE_SCAN_THROTTLING, false)
            .collectAsState(initial = false)

    val showExplanationDialog = rememberSaveable { mutableStateOf(false) }

    val developerSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (context.isWifiScanThrottled() == false) {
                coroutineScope.launch {
                    settings.edit { setBoolean(PreferenceKeys.IGNORE_SCAN_THROTTLING, true) }
                }
            }
        }

    LaunchedEffect(enabled.value) {
        /**
         * If Wi-Fi scan throttling has been re-enabled in the settings, change the settings value
         * so that the toggle will be off. There is no callback for this, so we need to check this
         * when rendering the composable
         *
         * This assumes that the scanner service checks from system settings whether Wi-Fi scanning
         * is throttled before starting scanning
         */
        if (context.isWifiScanThrottled() == true && enabled.value) {
            settings.edit { setBoolean(PreferenceKeys.IGNORE_SCAN_THROTTLING, false) }
        }
    }

    if (showExplanationDialog.value) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog.value = false },
            title = { Text(stringResource(id = R.string.wifi_scan_throttling)) },
            text = { Text(stringResource(id = R.string.wifi_scan_throttling_explanation)) },
            dismissButton = {
                TextButton(onClick = { showExplanationDialog.value = false }) {
                    Text(stringResource(id = R.string.no_thanks))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val developerSettingsIntent =
                            Intent(
                                android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                            )
                        val canOpenDeveloperSettings =
                            developerSettingsIntent.resolveActivity(context.packageManager) != null

                        if (canOpenDeveloperSettings) {
                            developerSettingsLauncher.launch(developerSettingsIntent)
                        }

                        showExplanationDialog.value = false
                    }
                ) {
                    Text(stringResource(id = R.string.ok))
                }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        )
    }

    ToggleWithAction(
        title = stringResource(id = R.string.settings_ignore_wifi_scan_throttling_title),
        description =
            stringResource(id = R.string.settings_ignore_wifi_scan_throttling_description),
        enabled = true,
        checked = enabled.value,
        action = { checked ->
            if (checked && context.isWifiScanThrottled() != false) {
                showExplanationDialog.value = true
            } else {
                settings.edit { setBoolean(PreferenceKeys.IGNORE_SCAN_THROTTLING, checked) }
            }
        },
    )
}
