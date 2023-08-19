package xyz.malkki.wifiscannerformls.ui.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun ToggleWithAction(title: String, checked: Boolean, action: suspend (Boolean) -> Unit) {
    val enabled = remember {
        mutableStateOf(true)
    }
    val coroutineScope = rememberCoroutineScope()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(modifier = Modifier.wrapContentSize(), text = title)
        Spacer(modifier = Modifier.weight(1.0f))
        Switch(
            modifier = Modifier.wrapContentSize(),
            enabled = enabled.value,
            checked = checked,
            onCheckedChange = { checked ->
                coroutineScope.launch {
                    enabled.value = false
                    action(checked)
                    enabled.value = true
                }
            }
        )
    }
}