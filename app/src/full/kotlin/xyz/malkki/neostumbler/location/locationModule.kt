package xyz.malkki.neostumbler.location

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.dsl.module
import timber.log.Timber
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.extensions.isGoogleApisAvailable

private fun DataStore<Preferences>.preferFusedLocation(): Boolean = runBlocking {
    data.map { it[booleanPreferencesKey(PreferenceKeys.PREFER_FUSED_LOCATION)] }.firstOrNull() !=
        false
}

val locationModule: Module = module {
    factory<LocationSource> {
        val context: Context = get()
        val settingsStore: DataStore<Preferences> = get(PREFERENCES)

        if (settingsStore.preferFusedLocation() && context.isGoogleApisAvailable()) {
            Timber.i("Using fused location source")
            FusedLocationSource(context)
        } else {
            Timber.i("Using platform location source")
            PlatformLocationSource(context)
        }
    }
}
