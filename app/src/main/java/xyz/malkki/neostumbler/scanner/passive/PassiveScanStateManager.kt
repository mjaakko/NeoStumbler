package xyz.malkki.neostumbler.scanner.passive

import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.geography.LatLng

private const val TIMESTAMP_SUFFIX = "_timestamp"

private const val LAST_LATITUDE_SUFFIX = "_lat"
private const val LAST_LONGITUDE_SUFFIX = "_lng"

/** Helper for handling passive scanning state to avoid creating useless reports */
class PassiveScanStateManager(
    private val passiveScanStateStore: DataStore<Preferences>,
    private val timeSource: () -> Long = SystemClock::elapsedRealtime,
) {
    enum class DataType {
        CELL,
        WIFI,
    }

    suspend fun getLastReportLocation(dataType: DataType): LatLng? {
        return passiveScanStateStore.data
            .map { data ->
                val lat = data[doublePreferencesKey(dataType.name + LAST_LATITUDE_SUFFIX)]
                val lng = data[doublePreferencesKey(dataType.name + LAST_LONGITUDE_SUFFIX)]

                if (lat != null && lng != null) {
                    LatLng(lat, lng)
                } else {
                    null
                }
            }
            .first()
    }

    suspend fun updateLastReportLocation(dataType: DataType, latLng: LatLng) {
        passiveScanStateStore.edit {
            it[doublePreferencesKey(dataType.name + LAST_LATITUDE_SUFFIX)] = latLng.latitude
            it[doublePreferencesKey(dataType.name + LAST_LONGITUDE_SUFFIX)] = latLng.longitude
        }
    }

    suspend fun getMaxTimestamp(dataType: DataType): Long? {
        val now = timeSource()

        return passiveScanStateStore.data
            .map { it[longPreferencesKey(dataType.name + TIMESTAMP_SUFFIX)] }
            .first()
            // If the timestamp is in the future, the device has rebooted and we can create a new
            // report -> return null
            ?.takeIf { it < now }
    }

    suspend fun updateMaxTimestamp(dataType: DataType, timestamp: Long) {
        passiveScanStateStore.edit {
            it[longPreferencesKey(dataType.name + TIMESTAMP_SUFFIX)] = timestamp
        }
    }

    suspend fun reset() {
        passiveScanStateStore.edit { it.clear() }
    }
}
