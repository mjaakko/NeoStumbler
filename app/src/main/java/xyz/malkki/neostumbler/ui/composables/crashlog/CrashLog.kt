package xyz.malkki.neostumbler.ui.composables.crashlog

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.crashlog.CrashLogManager
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ui.composables.shared.CenteredCircularProgressIndicator
import xyz.malkki.neostumbler.ui.composables.shared.Dialog
import xyz.malkki.neostumbler.utils.getBugReportUrl

@Composable
fun CrashLogDialog(crashLogManager: CrashLogManager = koinInject(), onClose: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    var selectedEntry by rememberSaveable { mutableStateOf<String?>(null) }

    if (selectedEntry != null) {
        CrashLogFileDialog(entry = selectedEntry!!, onCloseDialog = { selectedEntry = null })
    }

    Dialog(
        title = stringResource(R.string.crash_log_title),
        onDismissRequest = { onClose() },
        secondaryActions = {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        crashLogManager.clearEntries()

                        onClose()
                    }
                }
            ) {
                Text(stringResource(R.string.delete_all))
            }
        },
        primaryActions = {
            TextButton(onClick = { onClose() }) { Text(stringResource(R.string.cancel)) }
        },
    ) {
        CrashLog(onOpenEntry = { selectedEntry = it })
    }
}

@Composable
private fun CrashLog(
    crashLogManager: CrashLogManager = koinInject(),
    onOpenEntry: (String) -> Unit,
) {
    val crashLogEntries: State<List<String>?> =
        crashLogManager.getEntries().collectAsStateWithLifecycle(null)

    if (crashLogEntries.value == null) {
        CenteredCircularProgressIndicator()
    } else {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier.padding(bottom = 8.dp).verticalScroll(state = scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            crashLogEntries.value!!.forEachIndexed { idx, entry ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .wrapContentHeight()
                            .clickable(onClick = { onOpenEntry(entry) })
                            .padding(vertical = 8.dp)
                ) {
                    Text(text = stringResource(R.string.crash_log_entry_title, idx + 1))
                }
            }
        }
    }
}

@Composable
private fun CrashLogFileDialog(
    crashLogManager: CrashLogManager = koinInject(),
    entry: String,
    onCloseDialog: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    val coroutineScope = rememberCoroutineScope()

    val logs = produceState<String?>(null, entry) { value = crashLogManager.getLogsForEntry(entry) }

    Dialog(
        title = stringResource(R.string.crash_log_title),
        onDismissRequest = { onCloseDialog() },
        secondaryActions = {
            TextButton(
                enabled = logs.value != null,
                onClick = {
                    coroutineScope.launch {
                        clipboard.setClipEntry(
                            ClipData.newPlainText("NeoStumbler crash log", logs.value!!)
                                .toClipEntry()
                        )

                        context.showToast(
                            ContextCompat.getString(context, R.string.text_copied_to_clipboard)
                        )
                    }
                },
            ) {
                Text(text = stringResource(R.string.copy))
            }

            TextButton(
                enabled = logs.value != null,
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, getBugReportUrl(logs = logs.value!!).toUri())
                    )
                },
            ) {
                Text(text = stringResource(R.string.bug_report_button))
            }
        },
        primaryActions = {
            TextButton(onClick = { onCloseDialog() }) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    ) {
        if (logs.value == null) {
            CenteredCircularProgressIndicator()
        } else {
            Text(
                modifier =
                    Modifier.verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                text = logs.value!!,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                softWrap = false,
            )
        }
    }
}
