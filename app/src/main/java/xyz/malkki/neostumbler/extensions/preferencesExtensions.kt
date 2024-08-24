package xyz.malkki.neostumbler.extensions

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

inline fun <reified E : Enum<E>> Preferences.get(key: String): E? {
    return get(stringPreferencesKey(key))?.let {
        enumValueOf<E>(it)
    }
}