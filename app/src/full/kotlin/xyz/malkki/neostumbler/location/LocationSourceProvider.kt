package xyz.malkki.neostumbler.location

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.extensions.isGoogleApisAvailable

class LocationSourceProvider(private val context: Context) : KoinComponent {
    private val settingsStore: DataStore<Preferences> by inject<DataStore<Preferences>>(PREFERENCES)

    private fun preferFusedLocation(): Boolean = runBlocking {
        settingsStore.data
            .map { it[booleanPreferencesKey(PreferenceKeys.PREFER_FUSED_LOCATION)] }
            .firstOrNull() != false
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