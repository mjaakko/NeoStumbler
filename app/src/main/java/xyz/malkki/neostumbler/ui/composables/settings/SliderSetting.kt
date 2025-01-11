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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication

private fun DataStore<Preferences>.getValue(preferenceKey: String): Flow<Int?> = data.map {
    it[intPreferencesKey(preferenceKey)]
}

@Composable
fun SliderSetting(
    title: String,
    preferenceKey: String,
    range: IntRange,
    step: Int,
    valueFormatter: (Int) -> String,
    default: Int
) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val settingsStore = (context.applicationContext as StumblerApplication).settingsStore
    val preferenceValue = settingsStore.getValue(preferenceKey).collectAsState(initial = default)

    val sliderValue = remember(preferenceValue.value) {
        mutableIntStateOf(preferenceValue.value ?: default)
    }

    val dialogOpen = rememberSaveable { mutableStateOf(false) }

    if (dialogOpen.value) {
        BasicAlertDialog(
            onDismissRequest = {
                dialogOpen.value = false
            }
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        style = MaterialTheme.typography.titleLarge,
                        text = title,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Slider(
                            value = sliderValue.intValue.toFloat(),
                            onValueChange = {
                                sliderValue.intValue = it.toInt()
                            },
                            steps = ((range.endInclusive - range.start) / step) - 1,
                            valueRange = range.start.toFloat()..range.endInclusive.toFloat()
                        )

                        Text(
                            text = valueFormatter.invoke(sliderValue.intValue)
                        )
                    }

                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = {
                            dialogOpen.value = false

                            coroutineScope.launch {
                                settingsStore.updateData { prefs ->
                                    prefs.toMutablePreferences().apply {
                                        set(intPreferencesKey(preferenceKey), sliderValue.intValue)
                                    }
                                }
                            }
                        },
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                }
            }
        }
    }

    SettingsItem(
        title = title,
        description = valueFormatter.invoke(preferenceValue.value ?: default),
        onClick = {
            dialogOpen.value = true
        }
    )
}