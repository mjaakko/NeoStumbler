package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.db.DbPruneWorker

private const val ONE_MONTH = 30
private const val TWO_MONTHS = 60
private const val SIX_MONTHS = 180
private const val NEVER = -1

private val TITLES =
    mapOf(
        ONE_MONTH to R.string.db_prune_one_month,
        TWO_MONTHS to R.string.db_prune_two_months,
        SIX_MONTHS to R.string.db_prune_six_months,
        NEVER to R.string.db_prune_never,
    )

private fun DataStore<Preferences>.dbPruneMaxAgeDays(): Flow<Int> =
    data
        .map { preferences ->
            preferences[intPreferencesKey(PreferenceKeys.DB_PRUNE_DATA_MAX_AGE_DAYS)]
                ?: DbPruneWorker.DEFAULT_MAX_AGE_DAYS
        }
        .distinctUntilChanged()

@Composable
fun DbPruneSettings() {
    val context = LocalContext.current

    val settingsStore = koinInject<DataStore<Preferences>>(PREFERENCES)

    val dbPruneMaxAgeDays = settingsStore.dbPruneMaxAgeDays().collectAsState(initial = null)

    if (dbPruneMaxAgeDays.value != null) {
        MultiChoiceSettings(
            title = stringResource(id = R.string.db_prune_title),
            options = TITLES.keys,
            selectedOption = dbPruneMaxAgeDays.value!!,
            titleProvider = { ContextCompat.getString(context, TITLES[it]!!) },
            onValueSelected = { value ->
                settingsStore.updateData { prefs ->
                    prefs.toMutablePreferences().apply {
                        set(intPreferencesKey(PreferenceKeys.DB_PRUNE_DATA_MAX_AGE_DAYS), value)
                    }
                }
            },
        )
    }
}
