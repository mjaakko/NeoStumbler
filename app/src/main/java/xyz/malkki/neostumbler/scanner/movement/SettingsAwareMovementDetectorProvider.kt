package xyz.malkki.neostumbler.scanner.movement

import android.content.Context
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.location.LocationSourceProvider
import xyz.malkki.neostumbler.data.movement.LocationBasedMovementDetector
import xyz.malkki.neostumbler.data.movement.MovementDetector
import xyz.malkki.neostumbler.data.movement.MovementDetectorProvider
import xyz.malkki.neostumbler.data.movement.SignificantMotionMovementDetector
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getEnum

class SettingsAwareMovementDetectorProvider(
    private val context: Context,
    private val settings: Settings,
    private val locationSourceProvider: LocationSourceProvider,
) : MovementDetectorProvider {
    override suspend fun getMovementDetector(): MovementDetector {
        val type =
            settings
                .getSnapshotFlow()
                .map { settingsSnapshot ->
                    settingsSnapshot.getEnum<MovementDetectorType>(PreferenceKeys.MOVEMENT_DETECTOR)
                }
                .first() ?: MovementDetectorType.LOCATION

        return when (type) {
            MovementDetectorType.NONE -> ConstantMovementDetector
            MovementDetectorType.LOCATION ->
                LocationBasedMovementDetector(locationSourceProvider = locationSourceProvider)
            MovementDetectorType.SIGNIFICANT_MOTION ->
                SignificantMotionMovementDetector(
                    sensorManager = context.getSystemService<SensorManager>()!!,
                    locationSourceProvider = locationSourceProvider,
                )
        }
    }
}
