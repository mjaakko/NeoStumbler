package xyz.malkki.neostumbler.ui.composables.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.extensions.isWifiScanThrottled
import xyz.malkki.neostumbler.ui.composables.ToggleWithAction

private fun DataStore<Preferences>.ignoringWifiScanThrottling(): Flow<Boolean> =
    data
        .map { it[booleanPreferencesKey(PreferenceKeys.IGNORE_SCAN_THROTTLING)] }
        .distinctUntilChanged()
        .map { it == true }

@Composable
fun IgnoreScanThrottlingToggle() {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val settingsStore = koinInject<DataStore<Preferences>>(PREFERENCES)
    val enabled = settingsStore.ignoringWifiScanThrottling().collectAsState(initial = false)

    val showExplanationDialog = rememberSaveable { mutableStateOf(false) }

    val developerSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (context.isWifiScanThrottled() == false) {
                coroutineScope.launch {
                    settingsStore.edit {
                        it[booleanPreferencesKey(PreferenceKeys.IGNORE_SCAN_THROTTLING)] = true
                    }
                }
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
                            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
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
                settingsStore.edit {
                    it[booleanPreferencesKey(PreferenceKeys.IGNORE_SCAN_THROTTLING)] = checked
                }
            }
        },
    )
}
