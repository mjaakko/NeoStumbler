package xyz.malkki.neostumbler.ui.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.StumblerApplication

private fun DataStore<Preferences>.preferenceEnabled(preferenceKey: String, default: Boolean): Flow<Boolean> = data
    .map { it[booleanPreferencesKey(preferenceKey)] }
    .distinctUntilChanged()
    .map {
        it ?: default
    }

@Composable
fun SettingsToggle(title: String, preferenceKey: String, default: Boolean = false) {
    val context = LocalContext.current

    val settingsStore = (context.applicationContext as StumblerApplication).settingsStore
    val enabled = settingsStore.preferenceEnabled(preferenceKey, default).collectAsState(initial = default)

    ToggleWithAction(
        title = title,
        enabled = true,
        checked = enabled.value,
        action = { checked ->
            settingsStore.edit {
                it[booleanPreferencesKey(preferenceKey)] = checked
            }
        }
    )
}