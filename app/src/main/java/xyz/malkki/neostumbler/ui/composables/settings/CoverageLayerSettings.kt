package xyz.malkki.neostumbler.ui.composables.settings

import android.util.Patterns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.ui.composables.settings.SettingsItem
import xyz.malkki.neostumbler.ui.composables.settings.geosubmit.SuggestedServicesDialog

private fun DataStore<Preferences>.coverageLayerTileJsonUrl(): Flow<String?> = data
    .map { preferences ->
        preferences[stringPreferencesKey(PreferenceKeys.COVERAGE_TILE_JSON_URL)]
    }
    .distinctUntilChanged()

@Composable
fun CoverageLayerSettings() {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val settingsStore = (context.applicationContext as StumblerApplication).settingsStore
    val tileJsonUrl = settingsStore.coverageLayerTileJsonUrl().collectAsState(initial = null)

    val dialogOpen = rememberSaveable { mutableStateOf(false) }

    if (dialogOpen.value) {
        CoverageLayerDialog(
            currentTileJsonUrl = tileJsonUrl.value,
            onDialogClose = { newTileJsonUrl ->
                coroutineScope.launch {
                    settingsStore.updateData { prefs ->
                        prefs.toMutablePreferences().apply {
                            if (newTileJsonUrl.isNullOrEmpty()) {
                                remove(stringPreferencesKey(PreferenceKeys.COVERAGE_TILE_JSON_URL))
                            } else {
                                set(stringPreferencesKey(PreferenceKeys.COVERAGE_TILE_JSON_URL), newTileJsonUrl)
                            }
                        }
                    }
                    dialogOpen.value = false
                }
            }
        )
    }

    SettingsItem(
        title = stringResource(R.string.coverage_layer),
        description = tileJsonUrl.value ?: stringResource(R.string.coverage_layer_no_configured_tile_json_url),
        onClick = {
            dialogOpen.value = true
        }
    )
}

@Composable
private fun CoverageLayerDialog(currentTileJsonUrl: String?, onDialogClose: (String?) -> Unit) {
    val tileJsonUrl = rememberSaveable {
        mutableStateOf(currentTileJsonUrl)
    }

    val showSuggestedServicesDialog = rememberSaveable {
        mutableStateOf(false)
    }

    if (showSuggestedServicesDialog.value) {
        SuggestedServicesDialog(
            onServiceSelected = { service ->
                if (service != null)  {
                    tileJsonUrl.value = service.coverageTileJsonUrl
                }

                showSuggestedServicesDialog.value = false
            }
        )
    }

    BasicAlertDialog(
        onDismissRequest = { onDialogClose(null) }
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    style = MaterialTheme.typography.titleLarge,
                    text = stringResource(id = R.string.coverage_layer),
                )

                Spacer(modifier = Modifier.height(16.dp))


                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = tileJsonUrl.value ?: "",
                    onValueChange = { newTileJsonUrl ->
                        tileJsonUrl.value = newTileJsonUrl
                    },
                    label = { Text(text = stringResource(id = R.string.coverage_layer_tile_json_url)) },
                    singleLine = true
                )

                if (!tileJsonUrl.value.isValidUrl) {
                    Warning(R.string.no_valid_url_warning)
                }

                if (tileJsonUrl.value.isUnencryptedUrl) {
                    Warning(R.string.unencrypted_endpoint_warning)
                }


                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {
                        showSuggestedServicesDialog.value = true
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.suggested_services_title)
                    )
                }

                Row {
                    Spacer(modifier = Modifier.weight(1.0f))

                    TextButton(
                        onClick = { onDialogClose(tileJsonUrl.value) },
                        enabled = tileJsonUrl.value?.isValidUrl ?: false || tileJsonUrl.value?.isEmpty() ?: false
                    ) {
                        Text(text = stringResource(id = R.string.save))
                    }
                }
            }
        }
    }
}

private val String?.isValidUrl: Boolean
    get() = Patterns.WEB_URL.matcher(this ?: "").matches()

private val String?.isUnencryptedUrl: Boolean
    get() = this?.startsWith("http:") ?: false
