package xyz.malkki.neostumbler.ui.composables.settings

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.scanner.movement.MovementDetectorType

private val TITLES = mapOf(
    MovementDetectorType.NONE to R.string.movement_detection_none_title,
    MovementDetectorType.LOCATION to R.string.movement_detection_location_title,
    MovementDetectorType.SIGNIFICANT_MOTION to R.string.movement_detection_significant_motion_title,
)

private val DESCRIPTIONS = mapOf(
    MovementDetectorType.NONE to R.string.movement_detection_none_description,
    MovementDetectorType.LOCATION to R.string.movement_detection_location_description,
    MovementDetectorType.SIGNIFICANT_MOTION to R.string.movement_detection_significant_motion_description
)

private fun DataStore<Preferences>.movementDetector(): Flow<MovementDetectorType> = data
    .map { preferences ->
        preferences[stringPreferencesKey(PreferenceKeys.MOVEMENT_DETECTOR)]
    }
    .map { movementDetectorType ->
        try {
            movementDetectorType?.let {
                MovementDetectorType.valueOf(it)
            } ?: MovementDetectorType.NONE
        } catch (ex: Exception) {
            MovementDetectorType.NONE
        }
    }
    .distinctUntilChanged()

@Composable
fun MovementDetectorSettings() {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val settingsStore = (context.applicationContext as StumblerApplication).settingsStore

    val movementDetectorType = settingsStore.movementDetector().collectAsState(initial = null)

    val dialogOpen = remember { mutableStateOf(false) }

    if (dialogOpen.value && movementDetectorType.value != null) {
        MovementDetectorDialog(
            currentMovementDetector = movementDetectorType.value!!,
            onDialogClose = { newMovementDetectorType ->
                coroutineScope.launch {
                    settingsStore.updateData { prefs ->
                        prefs.toMutablePreferences().apply {
                            set(stringPreferencesKey(PreferenceKeys.MOVEMENT_DETECTOR), newMovementDetectorType.name)
                        }
                    }

                    dialogOpen.value = false
                }
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = movementDetectorType.value != null) {
                dialogOpen.value = true
            }
    ) {
        Column {
            Text(text = stringResource(id = R.string.movement_detection))
            Text(
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                text = movementDetectorType.value?.let { stringResource(id = TITLES[it]!!) } ?: ""
            )
        }
    }
}

private fun Context.significantMotionSensorAvailable(): Boolean {
    return getSystemService<SensorManager>()!!.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null
}

@Composable
private fun MovementDetectorDialog(currentMovementDetector: MovementDetectorType, onDialogClose: (MovementDetectorType) -> Unit) {
    val context = LocalContext.current

    val contentColor = LocalContentColor.current

    BasicAlertDialog(
        onDismissRequest = { onDialogClose(currentMovementDetector) }
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
                    text = stringResource(id = R.string.movement_detection),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.selectableGroup().padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MovementDetectorType.entries.forEach { movementDetectorType ->
                        val enabled = movementDetectorType != MovementDetectorType.SIGNIFICANT_MOTION || context.significantMotionSensorAvailable()

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .defaultMinSize(minHeight = 36.dp)
                                .selectable(
                                    enabled = enabled,
                                    selected = (currentMovementDetector == movementDetectorType),
                                    onClick = { onDialogClose(movementDetectorType) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                modifier = Modifier
                                    .align(Alignment.Top)
                                    .padding(top = 4.dp),
                                enabled = enabled,
                                selected = (currentMovementDetector == movementDetectorType),
                                onClick = null
                            )

                            Column(
                                modifier = Modifier
                                    //Disabled -> alpha 0.38f https://developer.android.com/develop/ui/compose/designsystems/material2-material3#emphasis-and
                                    .alpha(if (enabled) { 1f } else { 0.38f })
                                    .align(Alignment.Top)
                                    .padding(start = 16.dp)
                            ) {
                                Text(
                                    text = stringResource(id = TITLES[movementDetectorType]!!),
                                    style = MaterialTheme.typography.bodyMedium.merge()
                                )

                                Text(
                                    text = stringResource(id = DESCRIPTIONS[movementDetectorType]!!),
                                    fontWeight = FontWeight.Light,
                                    style = MaterialTheme.typography.bodySmall.merge()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}