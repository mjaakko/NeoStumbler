package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.ui.composables.ToggleWithAction

@Composable
fun SettingsToggle(
    title: String,
    description: String? = null,
    preferenceKey: String,
    default: Boolean = false,
    settings: Settings = koinInject<Settings>(),
) {
    val enabled = settings.getBooleanFlow(preferenceKey, default).collectAsState(initial = default)

    ToggleWithAction(
        title = title,
        description = description,
        enabled = true,
        checked = enabled.value,
        action = { checked -> settings.edit { setBoolean(preferenceKey, checked) } },
    )
}
