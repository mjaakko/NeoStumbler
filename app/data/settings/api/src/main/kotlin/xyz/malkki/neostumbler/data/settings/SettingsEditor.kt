package xyz.malkki.neostumbler.data.settings

interface SettingsEditor {
    fun setString(key: String, value: String)

    fun setStringSet(key: String, value: Set<String>)

    fun setBoolean(key: String, value: Boolean)

    fun setInt(key: String, value: Int)

    fun removeString(key: String)

    fun removeStringSet(key: String)

    fun removeBoolean(key: String)

    fun removeInt(key: String)
}

fun <E : Enum<E>> SettingsEditor.setEnum(key: String, value: E) = setString(key, value.name)

fun SettingsEditor.removeEnum(key: String) = removeString(key)
