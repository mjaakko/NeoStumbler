package xyz.malkki.neostumbler.ui.composables.settings.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getStringSetFlow
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.ui.composables.settings.SettingsItem

@Composable
fun WifiFilterSettings(settings: Settings = koinInject()) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val wifiFilterList by
        settings
            .getStringSetFlow(PreferenceKeys.WIFI_FILTER_LIST, emptySet())
            .collectAsStateWithLifecycle(emptySet())

    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    if (dialogOpen) {
        WifiFilterSettingsDialog(
            wifiFilterList = wifiFilterList,
            onWifiFilterListChanged = { newWifiFilterList ->
                coroutineScope.launch {
                    if (newWifiFilterList != null) {
                        settings.edit {
                            setStringSet(PreferenceKeys.WIFI_FILTER_LIST, newWifiFilterList)
                        }
                    }

                    dialogOpen = false
                }
            },
        )
    }

    SettingsItem(
        title = stringResource(R.string.wifi_filter_settings_title),
        description =
            context.getQuantityString(
                R.plurals.wifi_filter_settings_description,
                wifiFilterList.size,
                wifiFilterList.size,
            ),
        onClick = { dialogOpen = true },
    )
}

private const val TEXT_FIELD_ROWS = 10

@Composable
private fun WifiFilterSettingsDialog(
    wifiFilterList: Set<String>,
    onWifiFilterListChanged: (Set<String>?) -> Unit,
) {
    var textFieldValue by rememberSaveable { mutableStateOf(wifiFilterList.joinToString("\n")) }

    BasicAlertDialog(onDismissRequest = { onWifiFilterListChanged(null) }) {
        Surface(
            modifier = Modifier.wrapContentWidth().wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    style = MaterialTheme.typography.titleLarge,
                    text = stringResource(R.string.wifi_filter_settings_title),
                )

                TextField(
                    modifier = Modifier.padding(vertical = 8.dp),
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    supportingText = {
                        Text(text = stringResource(R.string.wifi_filter_settings_hint))
                    },
                    minLines = TEXT_FIELD_ROWS,
                    maxLines = TEXT_FIELD_ROWS,
                )

                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = {
                        val newWifiFilterList =
                            textFieldValue
                                .split('\n')
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .toSet()

                        onWifiFilterListChanged(newWifiFilterList)
                    },
                ) {
                    Text(text = stringResource(R.string.save))
                }
            }
        }
    }
}
