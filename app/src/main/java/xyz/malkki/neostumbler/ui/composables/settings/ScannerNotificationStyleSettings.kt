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
import xyz.malkki.neostumbler.data.settings.getEnumFlow
import xyz.malkki.neostumbler.data.settings.setEnum
import xyz.malkki.neostumbler.scanner.ScannerService

private val TITLES =
    mapOf(
        ScannerService.Companion.NotificationStyle.MINIMAL to
            R.string.notification_style_minimal_title,
        ScannerService.Companion.NotificationStyle.BASIC to R.string.notification_style_basic_title,
        ScannerService.Companion.NotificationStyle.DETAILED to
            R.string.notification_style_detailed_title,
    )

private val DESCRIPTIONS =
    mapOf(
        ScannerService.Companion.NotificationStyle.MINIMAL to
            R.string.notification_style_minimal_description,
        ScannerService.Companion.NotificationStyle.BASIC to
            R.string.notification_style_basic_description,
        ScannerService.Companion.NotificationStyle.DETAILED to
            R.string.notification_style_detailed_description,
    )

@Composable
fun ScannerNotificationStyleSettings(settings: Settings = koinInject()) {
    val context = LocalContext.current

    val notificationStyle =
        settings
            .getEnumFlow(
                PreferenceKeys.SCANNER_NOTIFICATION_STYLE,
                ScannerService.Companion.NotificationStyle.BASIC,
            )
            .collectAsState(initial = null)

    if (notificationStyle.value != null) {
        MultiChoiceSettings(
            title = stringResource(id = R.string.notification_style),
            options = ScannerService.Companion.NotificationStyle.entries,
            selectedOption = notificationStyle.value!!,
            titleProvider = { ContextCompat.getString(context, TITLES[it]!!) },
            descriptionProvider = { ContextCompat.getString(context, DESCRIPTIONS[it]!!) },
            onValueSelected = { newNotificationStyle ->
                settings.edit {
                    setEnum(PreferenceKeys.SCANNER_NOTIFICATION_STYLE, newNotificationStyle)
                }
            },
        )
    }
}
