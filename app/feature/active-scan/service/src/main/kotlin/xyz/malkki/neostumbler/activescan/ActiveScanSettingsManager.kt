package xyz.malkki.neostumbler.activescan

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.SettingsSnapshot

// By default, try to scan Wi-Fis every 50 meters
private const val DEFAULT_WIFI_SCAN_DISTANCE: Int = 50

// By default, try to scan cells every 120 meters
private const val DEFAULT_CELL_SCAN_DISTANCE: Int = 120

class ActiveScanSettingsManager(private val settings: Settings) {
    val activeScanSettings: Flow<ActiveScanSettings> =
        settings.getSnapshotFlow().map { settingsSnapshot ->
            settingsSnapshot.toActiveScanSettings()
        }

    /** @param wifiScanDistance In meters */
    suspend fun setWifiScanDistance(wifiScanDistance: Int) {
        settings.edit { setInt(ActiveScanPreferenceKeys.WIFI_SCAN_DISTANCE, wifiScanDistance) }
    }

    /** @param cellScanDistance In meters */
    suspend fun setCellScanDistance(cellScanDistance: Int) {
        settings.edit { setInt(ActiveScanPreferenceKeys.CELL_SCAN_DISTANCE, cellScanDistance) }
    }

    suspend fun setLowBatteryThreshold(lowBatteryThreshold: Int?) {
        settings.edit {
            if (lowBatteryThreshold != null) {
                setInt(
                    ActiveScanPreferenceKeys.PAUSE_ON_BATTERY_LEVEL_THRESHOLD,
                    lowBatteryThreshold,
                )
            } else {
                removeInt(ActiveScanPreferenceKeys.PAUSE_ON_BATTERY_LEVEL_THRESHOLD)
            }
        }
    }
}

internal fun SettingsSnapshot.toActiveScanSettings(): ActiveScanSettings {
    return ActiveScanSettings(
        wifiScanDistance =
            getInt(ActiveScanPreferenceKeys.WIFI_SCAN_DISTANCE) ?: DEFAULT_WIFI_SCAN_DISTANCE,
        cellScanDistance =
            getInt(ActiveScanPreferenceKeys.CELL_SCAN_DISTANCE) ?: DEFAULT_CELL_SCAN_DISTANCE,
        lowBatteryThreshold = getInt(ActiveScanPreferenceKeys.PAUSE_ON_BATTERY_LEVEL_THRESHOLD),
        ignoreWifiScanThrottling =
            getBoolean(ActiveScanPreferenceKeys.IGNORE_SCAN_THROTTLING) == true,
        pauseWhenOverheating = getBoolean(ActiveScanPreferenceKeys.PAUSE_WHEN_OVERHEATING) == true,
    )
}
