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
import xyz.malkki.neostumbler.geosubmit.MLSGeosubmit

private fun DataStore<Preferences>.geosubmitEndpointAndApiKey(): Flow<Pair<String, String?>> = data
    .map { preferences ->
        val endpoint = preferences[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT)] ?: MLSGeosubmit.DEFAULT_ENDPOINT
        val apiKey = preferences[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY)]

        endpoint to apiKey
    }
    .distinctUntilChanged()

@Composable
fun GeosubmitEndpointSettings() {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val settingsStore = (context.applicationContext as StumblerApplication).settingsStore
    val endpoint = settingsStore.geosubmitEndpointAndApiKey().collectAsState(initial = null)

    val dialogOpen = remember { mutableStateOf(false) }

    if (dialogOpen.value) {
        GeosubmitEndpointDialog(
            currentEndpoint = endpoint.value?.first,
            currentApiKey = endpoint.value?.second,
            onDialogClose = { endpointAndApiKey ->
                if (endpointAndApiKey != null) {
                    val (newEndpoint, newApiKey) = endpointAndApiKey

                    coroutineScope.launch {
                        settingsStore.edit { prefs ->
                            prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT)] = newEndpoint

                            if (newApiKey != null) {
                                prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY)] = newApiKey
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
            Text(fontSize = 12.sp, fontWeight = FontWeight.Light, text = endpoint.value?.first ?: "")
        }
    }
}

@Composable
private fun GeosubmitEndpointDialog(currentEndpoint: String?, currentApiKey: String?, onDialogClose: (Pair<String, String?>?) -> Unit) {
    val endpoint = remember {
        mutableStateOf(currentEndpoint)
    }
    val apiKey = remember {
        mutableStateOf(currentApiKey)
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
                            endpoint.value = MLSGeosubmit.DEFAULT_ENDPOINT
                            apiKey.value = null
                        }
                    ) {
                        Text(text = stringResource(id = R.string.reset))
                    }

                    Spacer(modifier = Modifier.weight(1.0f))

                    TextButton(
                        onClick = { onDialogClose(endpoint.value!! to apiKey.value) },
                        enabled = !endpoint.value.isNullOrBlank()
                    ) {
                        Text(text = stringResource(id = R.string.save))
                    }
                }
            }
        }
    }
}