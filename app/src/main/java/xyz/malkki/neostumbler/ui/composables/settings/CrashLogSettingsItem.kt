package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.crashlog.CrashLogManager
import xyz.malkki.neostumbler.ui.composables.crashlog.CrashLogDialog

@Composable
fun CrashLogSettingsItem(crashLogManager: CrashLogManager = koinInject()) {
    val crashLogEntries by crashLogManager.getEntries().collectAsStateWithLifecycle(emptyList())

    var showCrashLog by rememberSaveable { mutableStateOf(false) }

    if (showCrashLog) {
        CrashLogDialog(onClose = { showCrashLog = false })
    }

    if (crashLogEntries.isNotEmpty()) {
        SettingsItem(
            title = stringResource(R.string.crash_log_title),
            onClick = { showCrashLog = true },
        )
    }
}
