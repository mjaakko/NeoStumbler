package xyz.malkki.neostumbler.data.settings

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

internal class DataStoreSettingsSnapshot(private val preferences: Preferences) : SettingsSnapshot {
    override fun getString(key: String): String? {
        return preferences[stringPreferencesKey(key)]
    }

    override fun getStringSet(key: String): Set<String>? {
        return preferences[stringSetPreferencesKey(key)]
    }

    override fun getBoolean(key: String): Boolean? {
        return preferences[booleanPreferencesKey(key)]
    }

    override fun getInt(key: String): Int? {
        return preferences[intPreferencesKey(key)]
    }
}
