package xyz.malkki.neostumbler.ui.composables.settings

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.geosubmit.GeosubmitParams

private fun DataStore<Preferences>.geosubmitParams(): Flow<GeosubmitParams> = data
    .map { preferences ->
        val endpoint = preferences[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT)] ?: GeosubmitParams.DEFAULT_BASE_URL
        val path = preferences[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_PATH)] ?: GeosubmitParams.DEFAULT_PATH
        val apiKey = preferences[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY)]

        GeosubmitParams(endpoint, path, apiKey)
    }
    .distinctUntilChanged()

@Composable
fun GeosubmitEndpointSettings() {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val settingsStore = (context.applicationContext as StumblerApplication).settingsStore
    val params = settingsStore.geosubmitParams().collectAsState(initial = null)

    val dialogOpen = remember { mutableStateOf(false) }

    if (dialogOpen.value) {
        GeosubmitEndpointDialog(
            currentParams = params.value,
            onDialogClose = { newParams ->
                if (newParams != null) {
                    coroutineScope.launch {
                        settingsStore.edit { prefs ->
                            prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT)] = newParams.baseUrl
                            prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_PATH)] = newParams.path

                            if (newParams.apiKey != null) {
                                prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY)] = newParams.apiKey
                            } else {
                                prefs.remove(stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY))
                            }
                        }
                    }
                }
                
                dialogOpen.value = false
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            dialogOpen.value = true
        }
    ) {
        Column {
            Text(text = stringResource(id = R.string.endpoint))
            Text(fontSize = 12.sp, fontWeight = FontWeight.Light, text = params.value?.baseUrl ?: "")
        }
    }
}

@Composable
private fun GeosubmitEndpointDialog(currentParams: GeosubmitParams?, onDialogClose: (GeosubmitParams?) -> Unit) {
    val endpoint = remember {
        mutableStateOf(currentParams?.baseUrl)
    }
    val path = remember {
        mutableStateOf(currentParams?.path)
    }
    val apiKey = remember {
        mutableStateOf(currentParams?.apiKey)
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

                Row {
                    TextButton(
                        onClick = {
                            endpoint.value = GeosubmitParams.DEFAULT_BASE_URL
                            path.value = GeosubmitParams.DEFAULT_PATH
                            apiKey.value = null
                        }
                    ) {
                        Text(text = stringResource(id = R.string.reset))
                    }

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