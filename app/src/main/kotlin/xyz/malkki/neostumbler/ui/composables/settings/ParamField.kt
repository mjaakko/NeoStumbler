package xyz.malkki.neostumbler.ui.composables.settings

import android.util.Patterns
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import xyz.malkki.neostumbler.R

@Composable
fun ParamField(
    modifier: Modifier,
    label: String,
    state: TextFieldState,
    onDone: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    supportingText: (@Composable () -> Unit)? = null,
) {
    TextField(
        modifier = modifier,
        state = state,
        keyboardOptions =
            if (onDone != null) {
                keyboardOptions.copy(imeAction = ImeAction.Done)
            } else {
                keyboardOptions
            },
        onKeyboardAction = { defaultAction ->
            val action = onDone ?: defaultAction

            action()
        },
        label = { Text(text = label) },
        lineLimits = TextFieldLineLimits.SingleLine,
        supportingText = supportingText,
    )
}

@Composable
fun UrlField(
    modifier: Modifier,
    label: String,
    state: TextFieldState,
    onDone: (() -> Unit)? = null,
    /**
     * Supporting text to show under the field. Note that setting this param will hide the default
     * warnings
     */
    supportingText: (@Composable () -> Unit)? = null,
) {
    ParamField(
        modifier = modifier,
        label = label,
        state = state,
        onDone = onDone,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
        supportingText =
            if (supportingText != null) {
                { supportingText() }
            } else if (!state.text.isValidUrl) {
                { Warning(warningText = R.string.invalid_url) }
            } else if (state.text.isUnencryptedUrl) {
                { Warning(warningText = R.string.unencrypted_endpoint_warning) }
            } else {
                null
            },
    )
}

private val CharSequence.isValidUrl: Boolean
    get() = Patterns.WEB_URL.matcher(this).matches()

private val CharSequence.isUnencryptedUrl: Boolean
    get() = this.startsWith("http:")
