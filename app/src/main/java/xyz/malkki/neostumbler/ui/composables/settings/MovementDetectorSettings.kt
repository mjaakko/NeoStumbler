package xyz.malkki.neostumbler.ui.composables.settings

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getEnumFlow
import xyz.malkki.neostumbler.data.settings.setEnum
import xyz.malkki.neostumbler.scanner.movement.MovementDetectorType

private val TITLES =
    mapOf(
        MovementDetectorType.NONE to R.string.movement_detection_none_title,
        MovementDetectorType.LOCATION to R.string.movement_detection_location_title,
        MovementDetectorType.SIGNIFICANT_MOTION to
            R.string.movement_detection_significant_motion_title,
    )

private val DESCRIPTIONS =
    mapOf(
        MovementDetectorType.NONE to R.string.movement_detection_none_description,
        MovementDetectorType.LOCATION to R.string.movement_detection_location_description,
        MovementDetectorType.SIGNIFICANT_MOTION to
            R.string.movement_detection_significant_motion_description,
    )

@Composable
fun MovementDetectorSettings(settings: Settings = koinInject()) {
    val context = LocalContext.current

    val disabledOptions =
        remember(context) {
            if (context.significantMotionSensorAvailable()) {
                emptySet()
            } else {
                setOf(MovementDetectorType.SIGNIFICANT_MOTION)
            }
        }

    val movementDetectorType =
        settings
            .getEnumFlow(PreferenceKeys.MOVEMENT_DETECTOR, MovementDetectorType.LOCATION)
            .collectAsState(initial = null)

    if (movementDetectorType.value != null) {
        MultiChoiceSettings(
            title = stringResource(id = R.string.movement_detection),
            options = MovementDetectorType.entries,
            selectedOption = movementDetectorType.value!!,
            disabledOptions = disabledOptions,
            titleProvider = { ContextCompat.getString(context, TITLES[it]!!) },
            descriptionProvider = { ContextCompat.getString(context, DESCRIPTIONS[it]!!) },
            onValueSelected = { newMovementDetectorType ->
                settings.edit { setEnum(PreferenceKeys.MOVEMENT_DETECTOR, newMovementDetectorType) }
            },
        )
    }
}

private fun Context.significantMotionSensorAvailable(): Boolean {
    return getSystemService<SensorManager>()!!.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) !=
        null
}
