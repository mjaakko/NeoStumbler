package xyz.malkki.neostumbler.ui.composables.settings.geosubmit

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
import xyz.malkki.neostumbler.geosubmit.GeosubmitParams
import xyz.malkki.neostumbler.ui.composables.settings.SettingsItem

private fun DataStore<Preferences>.geosubmitParams(): Flow<GeosubmitParams?> = data
    .map { preferences ->
        val endpoint = preferences[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT)]
        val path = preferences[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_PATH)] ?: GeosubmitParams.DEFAULT_PATH
        val apiKey = preferences[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY)]

        if (endpoint != null) {
            GeosubmitParams(endpoint, path, apiKey)
        } else {
            null
        }
    }
    .distinctUntilChanged()

@Composable
fun GeosubmitEndpointSettings() {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val settingsStore = (context.applicationContext as StumblerApplication).settingsStore
    val params = settingsStore.geosubmitParams().collectAsState(initial = null)

    val dialogOpen = rememberSaveable { mutableStateOf(false) }

    if (dialogOpen.value) {
        GeosubmitEndpointDialog(
            currentParams = params.value,
            onDialogClose = { newParams ->
                if (newParams != null) {
                    coroutineScope.launch {
                        settingsStore.updateData { prefs ->
                            prefs.toMutablePreferences().apply {
                                set(stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT), newParams.baseUrl)
                                set(stringPreferencesKey(PreferenceKeys.GEOSUBMIT_PATH), newParams.path)

                                if (newParams.apiKey != null) {
                                    set(stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY), newParams.apiKey)
                                } else {
                                    remove(stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY))
                                }
                            }
                        }

                        dialogOpen.value = false
                    }
                } else {
                    dialogOpen.value = false
                }
            }
        )
    }

    SettingsItem(
        title = stringResource(R.string.endpoint),
        description = params.value?.baseUrl ?: stringResource(R.string.no_endpoint_configured),
        onClick = {
            dialogOpen.value = true
        }
    )
}

@Composable
private fun GeosubmitEndpointDialog(currentParams: GeosubmitParams?, onDialogClose: (GeosubmitParams?) -> Unit) {
    val endpoint = rememberSaveable {
        mutableStateOf(currentParams?.baseUrl)
    }
    val path = rememberSaveable {
        mutableStateOf(currentParams?.path ?: GeosubmitParams.DEFAULT_PATH)
    }
    val apiKey = rememberSaveable {
        mutableStateOf(currentParams?.apiKey)
    }

    val showSuggestedServicesDialog = rememberSaveable {
        mutableStateOf(false)
    }

    if (showSuggestedServicesDialog.value) {
        SuggestedServicesDialog(
            onServiceSelected = { service ->
                if (service != null)  {
                    endpoint.value = service.endpoint.baseUrl
                    path.value = service.endpoint.path
                    apiKey.value = service.endpoint.apiKey
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
                    text = stringResource(id = R.string.endpoint),
                )

                Spacer(modifier = Modifier.height(16.dp))

                //TODO: would be nice to have a validator here
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = endpoint.value ?: "",
                    onValueChange = { newEndpoint ->
                        endpoint.value = newEndpoint
                    },
                    label = { Text(text = stringResource(id = R.string.endpoint)) },
                    singleLine = true
                )

                if (endpoint.value.isUnencryptedUrl) {
                    UnencryptedEndpointWarning()
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = path.value ?: "",
                    onValueChange = { newPath ->
                        path.value = newPath
                    },
                    label = { Text(text = stringResource(id = R.string.path)) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = apiKey.value ?: "",
                    onValueChange = { newApiKey ->
                        apiKey.value = newApiKey
                    },
                    label = { Text(text = stringResource(id = R.string.api_key)) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                        onClick = { onDialogClose(GeosubmitParams(endpoint.value!!, path.value!!, apiKey.value)) },
                        enabled = !endpoint.value.isNullOrBlank() && !path.value.isNullOrBlank()
                    ) {
                        Text(text = stringResource(id = R.string.save))
                    }
                }
            }
        }
    }
}

private val String?.isUnencryptedUrl: Boolean
    get() = this?.startsWith("http:") == true