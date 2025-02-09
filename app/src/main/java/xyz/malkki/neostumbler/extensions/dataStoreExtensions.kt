package xyz.malkki.neostumbler.extensions

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

suspend fun <T> DataStore<Preferences>.getOrDefault(key: Preferences.Key<T>, default: T): T {
    return data.map { prefs -> prefs[key] ?: default }.first()
}

suspend inline fun <reified E : Enum<E>> DataStore<Preferences>.getOrDefault(
    key: Preferences.Key<String>,
    default: E,
): E {
    return enumValueOf<E>(getOrDefault(key, default.name))
}
