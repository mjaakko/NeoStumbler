package xyz.malkki.neostumbler.ui.composables.crashlog

import android.content.ClipData
import android.content.Intent
import android.text.format.DateFormat
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import java.time.Instant
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.crashlog.CrashLogEntry
import xyz.malkki.neostumbler.crashlog.CrashLogEntryContent
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
    val crashLogEntries: List<CrashLogEntry>? by
        crashLogManager.getEntries().collectAsStateWithLifecycle(null)

    if (crashLogEntries == null) {
        CenteredCircularProgressIndicator()
    } else {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier.padding(bottom = 8.dp).verticalScroll(state = scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val context = LocalContext.current

            val dateFormat = remember(context) { DateFormat.getDateFormat(context) }

            val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }

            crashLogEntries!!.forEach { entry ->
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .wrapContentHeight()
                            .clickable(onClick = { onOpenEntry(entry.id) })
                            .padding(vertical = 8.dp)
                ) {
                    val formattedTimestamp = entry.timestamp.formatted(dateFormat, timeFormat)

                    Text(text = stringResource(R.string.crash_log_entry_title, formattedTimestamp))
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

    val crashLogEntryContent by
        produceState<CrashLogEntryContent?>(null, entry) {
            value = crashLogManager.getEntryContent(entry)
        }

    val dateFormat = remember(context) { DateFormat.getDateFormat(context) }

    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }

    Dialog(
        title =
            crashLogEntryContent?.entry?.let {
                val formattedTimestamp = it.timestamp.formatted(dateFormat, timeFormat)

                stringResource(R.string.crash_log_entry_title, formattedTimestamp)
            } ?: "",
        onDismissRequest = { onCloseDialog() },
        secondaryActions = {
            TextButton(
                enabled = crashLogEntryContent != null,
                onClick = {
                    coroutineScope.launch {
                        clipboard.setClipEntry(
                            ClipData.newPlainText(
                                    "NeoStumbler crash log",
                                    crashLogEntryContent!!.content,
                                )
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
                enabled = crashLogEntryContent != null,
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            getBugReportUrl(logs = crashLogEntryContent?.content).toUri(),
                        )
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
        if (crashLogEntryContent == null) {
            CenteredCircularProgressIndicator()
        } else {
            Text(
                modifier =
                    Modifier.verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                text = crashLogEntryContent!!.content,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                softWrap = false,
            )
        }
    }
}

private fun Instant.formatted(
    dateFormat: java.text.DateFormat,
    timeFormat: java.text.DateFormat,
): String {
    return "${dateFormat.format(toEpochMilli())} ${timeFormat.format(toEpochMilli())}"
}
