package xyz.malkki.neostumbler.location

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences


class LocationSourceProvider(
    //This argument is needed so that the API matches the one in full variant
    private val settingsStore: DataStore<Preferences>
) {
    fun getLocationSource(context: Context): LocationSource {
        return PlatformLocationSource(context)
    }
}