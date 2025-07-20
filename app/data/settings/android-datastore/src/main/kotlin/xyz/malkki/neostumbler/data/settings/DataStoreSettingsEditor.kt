package xyz.malkki.neostumbler.data.settings

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

internal class DataStoreSettingsEditor(private val mutablePreferences: MutablePreferences) :
    SettingsEditor {
    override fun setString(key: String, value: String) {
        mutablePreferences[stringPreferencesKey(key)] = value
    }

    override fun setStringSet(key: String, value: Set<String>) {
        mutablePreferences[stringSetPreferencesKey(key)] = value
    }

    override fun setBoolean(key: String, value: Boolean) {
        mutablePreferences[booleanPreferencesKey(key)] = value
    }

    override fun setInt(key: String, value: Int) {
        mutablePreferences[intPreferencesKey(key)] = value
    }

    override fun removeString(key: String) {
        mutablePreferences.remove(stringPreferencesKey(key))
    }

    override fun removeStringSet(key: String) {
        mutablePreferences.remove(stringSetPreferencesKey(key))
    }

    override fun removeBoolean(key: String) {
        mutablePreferences.remove(booleanPreferencesKey(key))
    }

    override fun removeInt(key: String) {
        mutablePreferences.remove(intPreferencesKey(key))
    }
}
