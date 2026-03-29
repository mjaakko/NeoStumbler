package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
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
    val context = LocalContext.current

    val dbPruneMaxAgeDays =
        settings
            .getIntFlow(
                PreferenceKeys.DB_PRUNE_DATA_MAX_AGE_DAYS,
                DbPruneWorker.DEFAULT_MAX_AGE_DAYS,
            )
            .collectAsState(initial = null)

    val options = TITLES.keys
    val selectedOption =
        dbPruneMaxAgeDays.value
            ?.takeIf { it in options }
            ?: DbPruneWorker.DEFAULT_MAX_AGE_DAYS.takeIf { it in options }
            ?: NEVER

    if (dbPruneMaxAgeDays.value != null) {
        MultiChoiceSettings(
            title = stringResource(id = R.string.db_prune_title),
            options = options,
            selectedOption = selectedOption,
            titleProvider = { value -> ContextCompat.getString(context, TITLES[value] ?: R.string.db_prune_never) },
            onValueSelected = { value ->
                settings.edit { setInt(PreferenceKeys.DB_PRUNE_DATA_MAX_AGE_DAYS, value) }
            },
        )
    }
}
