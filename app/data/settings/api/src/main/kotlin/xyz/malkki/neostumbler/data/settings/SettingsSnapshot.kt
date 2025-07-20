package xyz.malkki.neostumbler.data.settings

interface SettingsSnapshot {
    fun getString(key: String): String?

    fun getStringSet(key: String): Set<String>?

    fun getBoolean(key: String): Boolean?

    fun getInt(key: String): Int?
}

inline fun <reified E : Enum<E>> SettingsSnapshot.getEnum(key: String): E? =
    getString(key)?.let { stringValue ->
        try {
            enumValueOf<E>(stringValue)
        } catch (_: Exception) {
            null
        }
    }
