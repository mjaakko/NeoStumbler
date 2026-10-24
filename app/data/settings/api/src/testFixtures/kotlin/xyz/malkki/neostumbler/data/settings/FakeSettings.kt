package xyz.malkki.neostumbler.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettings : Settings {
    private val settingsFlow = MutableStateFlow(FakeSettingsSnapshot(emptyMap()))

    override fun getSnapshotFlow(): Flow<SettingsSnapshot> {
        return settingsFlow
    }

    override suspend fun edit(editSettings: SettingsEditor.() -> Unit) {
        val updated = settingsFlow.value.values.toMutableMap()

        object : SettingsEditor {
                override fun setString(key: String, value: String) {
                    updated[key] = value
                }

                override fun setStringSet(key: String, value: Set<String>) {
                    updated[key] = value
                }

                override fun setBoolean(key: String, value: Boolean) {
                    updated[key] = value
                }

                override fun setInt(key: String, value: Int) {
                    updated[key] = value
                }

                override fun removeString(key: String) {
                    updated.remove(key)
                }

                override fun removeStringSet(key: String) {
                    updated.remove(key)
                }

                override fun removeBoolean(key: String) {
                    updated.remove(key)
                }

                override fun removeInt(key: String) {
                    updated.remove(key)
                }
            }
            .editSettings()

        settingsFlow.value = FakeSettingsSnapshot(updated)
    }
}

class FakeSettingsSnapshot(val values: Map<String, Any>) : SettingsSnapshot {
    override fun getString(key: String): String? {
        return values[key] as String?
    }

    override fun getStringSet(key: String): Set<String>? {
        return values[key] as Set<String>?
    }

    override fun getBoolean(key: String): Boolean? {
        return values[key] as Boolean?
    }

    override fun getInt(key: String): Int? {
        return values[key] as Int?
    }
}
