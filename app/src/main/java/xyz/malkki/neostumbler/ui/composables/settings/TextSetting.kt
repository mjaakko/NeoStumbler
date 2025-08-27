package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getStringFlow

@Composable
fun TextSetting(
    label: String,
    key: String,
    default: String = "",
    filter: (String) -> String = { it },
    isError: (String) -> Boolean = { false },
    onDone: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    settings: Settings = koinInject<Settings>(),
    modifier: Modifier = Modifier,
) {
    val textFlow = remember { MutableStateFlow("") }

    LaunchedEffect(Unit) {
        val initialText = settings.getStringFlow(key, default).first()
        textFlow.emit(initialText)
        textFlow.collectLatest {
            settings.edit {
                setString(key, it)
            }
        }
    }

    val text = textFlow.collectAsState().value

    TextField(
        modifier = modifier.fillMaxWidth(),
        value = text,
        onValueChange = { newValue: String ->
            textFlow.tryEmit(filter(newValue))
        },
        isError = isError(text),
        keyboardActions = onDone.let { KeyboardActions { onDone() } } ?: KeyboardActions.Default,
        keyboardOptions =
            if (onDone != null) {
                keyboardOptions.copy(imeAction = ImeAction.Done)
            } else {
                keyboardOptions
            },
        label = { Text(text = label) },
        singleLine = true,
    )
}
