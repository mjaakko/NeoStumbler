package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.ui.composables.ToggleWithAction

private fun DataStore<Preferences>.preferenceEnabled(
    preferenceKey: String,
    default: Boolean,
): Flow<Boolean> =
    data
        .map { it[booleanPreferencesKey(preferenceKey)] }
        .distinctUntilChanged()
        .map { it ?: default }

@Composable
fun SettingsToggle(
    title: String,
    description: String? = null,
    preferenceKey: String,
    default: Boolean = false,
) {
    val settingsStore = koinInject<DataStore<Preferences>>(PREFERENCES)

    val enabled =
        settingsStore.preferenceEnabled(preferenceKey, default).collectAsState(initial = default)

    ToggleWithAction(
        title = title,
        description = description,
        enabled = true,
        checked = enabled.value,
        action = { checked ->
            settingsStore.edit { it[booleanPreferencesKey(preferenceKey)] = checked }
        },
    )
}
