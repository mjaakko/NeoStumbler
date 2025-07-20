package xyz.malkki.neostumbler.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettings(private val dataStore: DataStore<Preferences>) : Settings {
    override fun getSnapshotFlow(): Flow<SettingsSnapshot> {
        return dataStore.data.map { preferences -> DataStoreSettingsSnapshot(preferences) }
    }

    override suspend fun edit(editSettings: SettingsEditor.() -> Unit) {
        dataStore.edit { mutablePreferences ->
            DataStoreSettingsEditor(mutablePreferences).apply(editSettings)
        }
    }
}
