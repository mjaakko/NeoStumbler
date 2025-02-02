package xyz.malkki.neostumbler.ui.composables

import android.text.SpannedString
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.getSpans
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.extensions.getTextCompat
import xyz.malkki.neostumbler.utils.OneTimeActionHelper
import xyz.malkki.neostumbler.utils.SuggestedService

private const val MLS_WARNING = "mls_warning"

//ID from suggested_services.json
private const val DEFAULT_SERVICE_ID = "beacondb"

@Composable
fun MLSWarningDialog() {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val oneTimeActionHelper = OneTimeActionHelper(context.applicationContext as StumblerApplication)

    val settingsStore = (context.applicationContext as StumblerApplication).settingsStore

    val warningShown = oneTimeActionHelper.hasActionBeenShownFlow(MLS_WARNING).collectAsState(
        initial = true
    )

    val defaultServiceParams = produceState<SuggestedService?>(null) {
        value = withContext(Dispatchers.IO) {
            SuggestedService.getSuggestedServices(context).find { it.id == DEFAULT_SERVICE_ID }
        }
    }

    val mlsWarningText = (context.getTextCompat(R.string.mls_warning_text) as SpannedString).let { spannedString ->
        val privacyPolicyLink = spannedString.getSpans<android.text.Annotation>().first()

        buildAnnotatedString {
            append(spannedString)

            addLink(
                url = LinkAnnotation.Url(defaultServiceParams.value?.termsOfUse ?: ""),
                start = spannedString.getSpanStart(privacyPolicyLink),
                end = spannedString.getSpanEnd(privacyPolicyLink)
            )
            addStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.primary),
                start = spannedString.getSpanStart(privacyPolicyLink),
                end = spannedString.getSpanEnd(privacyPolicyLink)
            )
        }
    }

    if (!warningShown.value) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(id = R.string.mls_warning_title)) },
            text = { Text(text = mlsWarningText) },
            confirmButton = {
                TextButton(
                    enabled = defaultServiceParams.value != null,
                    onClick = {
                        coroutineScope.launch {
                            settingsStore.updateData { prefs ->
                                val (baseUrl, path) = defaultServiceParams.value!!.endpoint

                                prefs.toMutablePreferences().apply {
                                    set(stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT), baseUrl)
                                    set(stringPreferencesKey(PreferenceKeys.GEOSUBMIT_PATH), path)

                                    remove(stringPreferencesKey(PreferenceKeys.GEOSUBMIT_API_KEY))
                                }
                            }

                            oneTimeActionHelper.markActionShown(MLS_WARNING)
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            oneTimeActionHelper.markActionShown(MLS_WARNING)
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.no))
                }
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }
}