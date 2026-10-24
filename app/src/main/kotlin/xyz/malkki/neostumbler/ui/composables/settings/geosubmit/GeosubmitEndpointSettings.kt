package xyz.malkki.neostumbler.ui.composables.settings.geosubmit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.ichnaea.IchnaeaParams
import xyz.malkki.neostumbler.ichnaeaupload.IchnaeaClientProvider
import xyz.malkki.neostumbler.ui.composables.settings.ParamField
import xyz.malkki.neostumbler.ui.composables.settings.SettingsItem
import xyz.malkki.neostumbler.ui.composables.settings.UrlField

@Composable
fun GeosubmitEndpointSettings(ichnaeaClientProvider: IchnaeaClientProvider = koinInject()) {
    val coroutineScope = rememberCoroutineScope()

    val params =
        ichnaeaClientProvider.ichnaeaParams.collectAsStateWithLifecycle(initialValue = null)

    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    if (dialogOpen) {
        GeosubmitEndpointDialog(
            currentParams = params.value,
            onDialogClose = { newParams ->
                if (newParams != null) {
                    coroutineScope.launch {
                        ichnaeaClientProvider.setIchnaeaParams(newParams)

                        dialogOpen = false
                    }
                } else {
                    dialogOpen = false
                }
            },
        )
    }

    SettingsItem(
        title = stringResource(R.string.endpoint),
        description = params.value?.baseUrl ?: stringResource(R.string.no_endpoint_configured),
        onClick = { dialogOpen = true },
    )
}

@Composable
private fun GeosubmitEndpointDialog(
    currentParams: IchnaeaParams?,
    onDialogClose: (IchnaeaParams?) -> Unit,
) {
    val endpoint = rememberTextFieldState(currentParams?.baseUrl.orEmpty())
    val geosubmitPath =
        rememberTextFieldState(
            currentParams?.submissionPath ?: IchnaeaParams.DEFAULT_SUBMISSION_PATH
        )
    val geolocatePath =
        rememberTextFieldState(currentParams?.locatePath ?: IchnaeaParams.DEFAULT_LOCATE_PATH)
    val apiKey = rememberTextFieldState(currentParams?.apiKey.orEmpty())

    var showSuggestedServicesDialog by rememberSaveable { mutableStateOf(false) }

    if (showSuggestedServicesDialog) {
        SuggestedServicesDialog(
            onServiceSelected = { service ->
                if (service != null) {
                    endpoint.setTextAndPlaceCursorAtEnd(service.endpoint.baseUrl)
                    geosubmitPath.setTextAndPlaceCursorAtEnd(service.endpoint.geosubmitPath)
                    geolocatePath.setTextAndPlaceCursorAtEnd(
                        service.endpoint.geolocatePath.orEmpty()
                    )
                    apiKey.setTextAndPlaceCursorAtEnd(service.endpoint.apiKey.orEmpty())
                }

                showSuggestedServicesDialog = false
            }
        )
    }

    BasicAlertDialog(onDismissRequest = { onDialogClose(null) }) {
        Surface(
            modifier = Modifier.wrapContentWidth().wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    style = MaterialTheme.typography.titleLarge,
                    text = stringResource(id = R.string.endpoint),
                )

                Column(
                    modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    UrlField(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.endpoint),
                        state = endpoint,
                    )

                    ParamField(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.submission_path),
                        state = geosubmitPath,
                    )

                    ParamField(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.locate_path),
                        state = geolocatePath,
                    )

                    ParamField(
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.api_key),
                        state = apiKey,
                    )
                }

                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = { showSuggestedServicesDialog = true },
                ) {
                    Text(text = stringResource(id = R.string.suggested_services_title))
                }

                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = {
                        onDialogClose(
                            IchnaeaParams(
                                baseUrl = endpoint.text.toString(),
                                submissionPath = geosubmitPath.text.toString(),
                                locatePath =
                                    geolocatePath.text.toString().takeIf { it.isNotBlank() },
                                apiKey = apiKey.text.toString().takeIf { it.isNotBlank() },
                            )
                        )
                    },
                    enabled = endpoint.text.isNotBlank() && geosubmitPath.text.isNotBlank(),
                ) {
                    Text(text = stringResource(id = R.string.save))
                }
            }
        }
    }
}
