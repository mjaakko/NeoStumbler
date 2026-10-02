package xyz.malkki.neostumbler.ui.composables.settings.privacy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.data.restrictedarea.RestrictedAreaManager
import xyz.malkki.neostumbler.ui.composables.settings.SettingsItem

@Composable
fun RestrictedAreaSettings(
    restrictedAreaManager: RestrictedAreaManager = koinInject(),
    openRestrictedAreas: () -> Unit,
) {
    val restrictedAreas by
        restrictedAreaManager.restrictedAreas.collectAsStateWithLifecycle(
            initialValue = emptyList()
        )

    SettingsItem(
        title = stringResource(R.string.restricted_areas_settings_title),
        description =
            pluralStringResource(
                R.plurals.restricted_areas_settings_description,
                restrictedAreas.size,
                restrictedAreas.size,
            ),
        onClick = openRestrictedAreas,
    )
}
