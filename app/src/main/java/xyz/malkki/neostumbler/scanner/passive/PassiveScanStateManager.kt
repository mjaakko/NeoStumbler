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

private const val WIFI_TIMESTAMP = "wifi"
private const val CELL_TIMESTAMP = "cell"

private const val LAST_LATITUDE = "lat"
private const val LAST_LONGITUDE = "lng"

/** Helper for handling passive scanning state to avoid creating useless reports */
class PassiveScanStateManager(private val passiveScanStateStore: DataStore<Preferences>) {
    suspend fun getLastReportLocation(): LatLng? {
        return passiveScanStateStore.data
            .map { data ->
                val lat = data[doublePreferencesKey(LAST_LATITUDE)]
                val lng = data[doublePreferencesKey(LAST_LONGITUDE)]

                if (lat != null && lng != null) {
                    LatLng(lat, lng)
                } else {
                    null
                }
            }
            .first()
    }

    suspend fun updateLastReportLocation(latLng: LatLng) {
        passiveScanStateStore.edit {
            it[doublePreferencesKey(LAST_LATITUDE)] = latLng.latitude
            it[doublePreferencesKey(LAST_LONGITUDE)] = latLng.longitude
        }
    }

    suspend fun getMaxWifiTimestamp(): Long? {
        val now = SystemClock.elapsedRealtime()

        return passiveScanStateStore.data
            .map { it[longPreferencesKey(WIFI_TIMESTAMP)] }
            .first()
            // If the timestamp is in the future, the device has rebooted and we can create a new
            // report -> return null
            ?.takeIf { it < now }
    }

    suspend fun updateMaxWifiTimestamp(timestamp: Long) {
        passiveScanStateStore.edit { it[longPreferencesKey(WIFI_TIMESTAMP)] = timestamp }
    }

    suspend fun getMaxCellTimestamp(): Long? {
        val now = SystemClock.elapsedRealtime()

        return passiveScanStateStore.data
            .map { it[longPreferencesKey(CELL_TIMESTAMP)] }
            .first()
            // If the timestamp is in the future, the device has rebooted and we can create a new
            // report -> return null
            ?.takeIf { it < now }
    }

    suspend fun updateMaxCellTimestamp(timestamp: Long) {
        passiveScanStateStore.edit { it[longPreferencesKey(CELL_TIMESTAMP)] = timestamp }
    }

    suspend fun reset() {
        passiveScanStateStore.edit { it.clear() }
    }
}
