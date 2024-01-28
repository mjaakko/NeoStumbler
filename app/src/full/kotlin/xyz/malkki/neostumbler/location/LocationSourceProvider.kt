package xyz.malkki.neostumbler.location

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.extensions.isGoogleApisAvailable

class LocationSourceProvider(private val context: Context) {
    private val settingsStore = (context.applicationContext as StumblerApplication).settingsStore

    private fun preferFusedLocation(): Boolean = runBlocking {
        settingsStore.data
            .map { it[booleanPreferencesKey(PreferenceKeys.PREFER_FUSED_LOCATION)] }
            .firstOrNull() ?: true
    }

    fun getLocationSource(): LocationSource {
        return if (preferFusedLocation() && context.isGoogleApisAvailable()) {
            Timber.i("Using fused location source")
            FusedLocationSource(context)
        } else {
            Timber.i("Using platform location source")
            PlatformLocationSource(context)
        }
    }
}