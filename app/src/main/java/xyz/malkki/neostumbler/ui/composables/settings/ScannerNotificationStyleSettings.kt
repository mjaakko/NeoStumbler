package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.extensions.get
import xyz.malkki.neostumbler.scanner.ScannerService

private val TITLES = mapOf(
    ScannerService.Companion.NotificationStyle.MINIMAL to R.string.notification_style_minimal_title,
    ScannerService.Companion.NotificationStyle.BASIC to R.string.notification_style_basic_title,
    ScannerService.Companion.NotificationStyle.DETAILED to R.string.notification_style_detailed_title,
)

private val DESCRIPTIONS = mapOf(
    ScannerService.Companion.NotificationStyle.MINIMAL to R.string.notification_style_minimal_description,
    ScannerService.Companion.NotificationStyle.BASIC to R.string.notification_style_basic_description,
    ScannerService.Companion.NotificationStyle.DETAILED to R.string.notification_style_detailed_description
)

private fun DataStore<Preferences>.scannerNotificationStyle(): Flow<ScannerService.Companion.NotificationStyle> = data
    .map { preferences ->
        preferences.get<ScannerService.Companion.NotificationStyle>(PreferenceKeys.SCANNER_NOTIFICATION_STYLE)
            ?: ScannerService.Companion.NotificationStyle.BASIC
    }
    .distinctUntilChanged()

@Composable
fun ScannerNotificationStyleSettings() {
    val context = LocalContext.current

    val settingsStore = (context.applicationContext as StumblerApplication).settingsStore
    
    val notificationStyle = settingsStore.scannerNotificationStyle().collectAsState(initial = null)

    if (notificationStyle.value != null) {
        MultiChoiceSettings(
            title = stringResource(id = R.string.notification_style),
            options = ScannerService.Companion.NotificationStyle.entries,
            selectedOption = notificationStyle.value!!,
            titleProvider = { context.getString(TITLES[it]!!) },
            descriptionProvider = { context.getString(DESCRIPTIONS[it]!!) },
            onValueSelected = { newNotificationStyle ->
                settingsStore.updateData { prefs ->
                    prefs.toMutablePreferences().apply {
                        set(stringPreferencesKey(PreferenceKeys.SCANNER_NOTIFICATION_STYLE), newNotificationStyle.name)
                    }
                }
            }
        )
    }
}