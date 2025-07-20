package xyz.malkki.neostumbler.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface Settings {
    fun getSnapshotFlow(): Flow<SettingsSnapshot>

    suspend fun edit(editSettings: SettingsEditor.() -> Unit)
}

fun Settings.getStringFlow(key: String, default: String): Flow<String> =
    getSnapshotFlow().map { prefs -> prefs.getString(key) ?: default }.distinctUntilChanged()

fun Settings.getStringSetFlow(key: String, default: Set<String>): Flow<Set<String>> =
    getSnapshotFlow().map { prefs -> prefs.getStringSet(key) ?: default }.distinctUntilChanged()

fun Settings.getBooleanFlow(key: String, default: Boolean): Flow<Boolean> =
    getSnapshotFlow().map { prefs -> prefs.getBoolean(key) ?: default }.distinctUntilChanged()

fun Settings.getIntFlow(key: String, default: Int): Flow<Int> =
    getSnapshotFlow().map { prefs -> prefs.getInt(key) ?: default }.distinctUntilChanged()

inline fun <reified E : Enum<E>> Settings.getEnumFlow(key: String, default: E): Flow<E> =
    getSnapshotFlow().map { prefs -> prefs.getEnum(key) ?: default }.distinctUntilChanged()
