package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getIntFlow

@Composable
fun SliderSetting(
    title: String,
    preferenceKey: String,
    range: IntRange,
    step: Int = 1,
    valueFormatter: @Composable (Int) -> String = { it.toString() },
    default: Int,
    settings: Settings = koinInject<Settings>(),
    saveButtonText: String = stringResource(R.string.save),
) {
    val coroutineScope = rememberCoroutineScope()

    val preferenceValue by
        settings
            .getIntFlow(preferenceKey, default)
            .collectAsStateWithLifecycle(initialValue = default)

    var sliderValue by remember(preferenceValue) { mutableIntStateOf(preferenceValue) }

    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    if (dialogOpen) {
        BasicAlertDialog(
            onDismissRequest = {
                dialogOpen = false

                // Reset slider value to default when dialog is closed without saving
                sliderValue = preferenceValue
            }
        ) {
            Surface(
                modifier = Modifier.wrapContentWidth().wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(style = MaterialTheme.typography.titleLarge, text = title)

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Slider(
                            value = sliderValue.toFloat(),
                            onValueChange = {
                                // roundToInt to avoid problems with floating point errors
                                sliderValue = it.roundToInt()
                            },
                            steps = ((range.last - range.first) / step) - 1,
                            valueRange = range.first.toFloat()..range.last.toFloat(),
                        )

                        Text(
                            text = valueFormatter.invoke(sliderValue),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = {
                            coroutineScope.launch {
                                settings.edit { setInt(preferenceKey, sliderValue) }

                                dialogOpen = false
                            }
                        },
                    ) {
                        Text(text = saveButtonText)
                    }
                }
            }
        }
    }

    SettingsItem(
        title = title,
        description = valueFormatter.invoke(preferenceValue),
        onClick = { dialogOpen = true },
    )
}
