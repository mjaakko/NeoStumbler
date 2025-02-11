package xyz.malkki.neostumbler.ui.composables.settings

import android.util.Patterns
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import xyz.malkki.neostumbler.R

@Composable
fun ParamField(
    label: String,
    state: MutableState<String?>,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    TextField(
        modifier = modifier,
        value = state.value ?: "",
        onValueChange = { newValue -> state.value = newValue },
        label = { Text(text = label) },
        singleLine = true,
    )
}

@Composable
fun UrlField(
    label: String,
    state: MutableState<String?>,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        ParamField(label = label, state = state, modifier = modifier)

        if (!state.value.isValidUrl) {
            Warning(warningText = R.string.invalid_url)
        } else if (state.value.isUnencryptedUrl) {
            Warning(warningText = R.string.unencrypted_endpoint_warning)
        }
    }
}

private val String?.isValidUrl: Boolean
    get() = Patterns.WEB_URL.matcher(this ?: "").matches()

private val String?.isUnencryptedUrl: Boolean
    get() = this?.startsWith("http:") == true
