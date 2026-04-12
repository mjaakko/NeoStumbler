package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getIntFlow
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

@Composable
fun DbPruneSettings(settings: Settings = koinInject()) {
    val dbPruneMaxAgeDays by
        settings
            .getIntFlow(
                PreferenceKeys.DB_PRUNE_DATA_MAX_AGE_DAYS,
                DbPruneWorker.DEFAULT_MAX_AGE_DAYS,
            )
            .collectAsStateWithLifecycle(initialValue = null)

    if (dbPruneMaxAgeDays != null) {
        MultiChoiceSettings(
            title = stringResource(id = R.string.db_prune_title),
            options = TITLES.keys,
            selectedOption = dbPruneMaxAgeDays!!,
            titleProvider = { value ->
                if (value in TITLES) {
                    stringResource(TITLES[value]!!)
                } else {
                    // Fallback for unsupported values
                    pluralStringResource(R.plurals.db_prune_custom_days, value, value)
                }
            },
            onValueSelected = { value ->
                settings.edit { setInt(PreferenceKeys.DB_PRUNE_DATA_MAX_AGE_DAYS, value) }
            },
        )
    }
}
