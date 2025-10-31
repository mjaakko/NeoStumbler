package xyz.malkki.neostumbler.ui.composables.settings.geosubmit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.ui.composables.shared.Link
import xyz.malkki.neostumbler.utils.SuggestedService

@Composable
fun SuggestedServicesDialog(onServiceSelected: (SuggestedService?) -> Unit) {
    val context = LocalContext.current

    val suggestedServices =
        produceState<List<SuggestedService>?>(null) {
            value = withContext(Dispatchers.IO) { SuggestedService.getSuggestedServices(context) }
        }

    val expanded = rememberSaveable { mutableStateOf(false) }

    val selectedService = rememberSaveable { mutableIntStateOf(0) }

    BasicAlertDialog(onDismissRequest = { onServiceSelected(null) }) {
        Surface(
            modifier = Modifier.wrapContentWidth().wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    style = MaterialTheme.typography.titleLarge,
                    text = stringResource(id = R.string.suggested_services_title),
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (suggestedServices.value == null) {
                    LoadingIndicator()
                } else {
                    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                        ExposedDropdownMenuBox(
                            expanded = expanded.value,
                            onExpandedChange = { expanded.value = it },
                        ) {
                            TextField(
                                value = suggestedServices.value!![selectedService.intValue].name,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = expanded.value
                                    )
                                },
                                modifier =
                                    Modifier.menuAnchor(
                                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable
                                    ),
                            )

                            ExposedDropdownMenu(
                                expanded = expanded.value,
                                onDismissRequest = { expanded.value = false },
                            ) {
                                suggestedServices.value!!.forEachIndexed { index, suggestedService
                                    ->
                                    DropdownMenuItem(
                                        text = { Text(text = suggestedService.name) },
                                        onClick = {
                                            selectedService.intValue = index
                                            expanded.value = false
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SuggestedServiceDetails(
                            service = suggestedServices.value!![selectedService.intValue]
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row {
                    TextButton(onClick = { onServiceSelected(null) }) {
                        Text(text = stringResource(id = R.string.cancel))
                    }

                    Spacer(modifier = Modifier.weight(1.0f))

                    TextButton(
                        onClick = {
                            onServiceSelected(suggestedServices.value!![selectedService.intValue])
                        }
                    ) {
                        Text(text = stringResource(id = R.string.use))
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun SuggestedServiceDetails(service: SuggestedService) {
    Text(text = service.name, style = MaterialTheme.typography.titleMedium)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text =
                buildAnnotatedString {
                    append(stringResource(id = R.string.suggested_service_hosted_by))

                    append(" ")

                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(service.hostedBy)
                    }
                },
            style = MaterialTheme.typography.bodySmall,
        )

        Link(text = stringResource(id = R.string.suggested_service_website), url = service.website)

        Link(
            text = stringResource(id = R.string.suggested_service_terms_of_use),
            url = service.termsOfUse,
        )
    }
}
