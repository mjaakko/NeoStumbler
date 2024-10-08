package xyz.malkki.neostumbler.ui.composables

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.extensions.selectedDateRange
import xyz.malkki.neostumbler.geosubmit.ReportSendWorker
import xyz.malkki.neostumbler.utils.SelectableDatesFromSet
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private fun getSelectableDatesSet(context: Context): LiveData<Set<LocalDate>> {
    return (context.applicationContext as StumblerApplication).reportDb.reportDao()
        .getReportDates()
        .map { it.toSet() }
}

@Composable
fun ReportReuploadButton() {
    val context = LocalContext.current

    val enqueuedUploadWork = remember { mutableStateOf<UUID?>(null) }

    EffectOnWorkCompleted(
        workId = enqueuedUploadWork.value,
        onWorkSuccess = { workInfo ->
            val reportsUploaded = workInfo.outputData.getInt(ReportSendWorker.OUTPUT_REPORTS_SENT, 0)
            Toast.makeText(context, ContextCompat.getString(context, R.string.toast_reports_uploaded).format(reportsUploaded), Toast.LENGTH_SHORT).show()
        },
        onWorkFailed = {
            Toast.makeText(context, ContextCompat.getString(context, R.string.toast_reports_upload_failed), Toast.LENGTH_SHORT).show()
        }
    )

    val selectableDates = getSelectableDatesSet(context).observeAsState()

    val dialogOpen = remember { mutableStateOf(false) }

    val dateRangePickerState = rememberDateRangePickerState(selectableDates = SelectableDatesFromSet(selectableDates::value))

    val selectedDates = dateRangePickerState.selectedDateRange()

    if (dialogOpen.value) {
        DatePickerDialog(
            onDismissRequest = {
                dialogOpen.value = false
            },
            confirmButton = {
                Button(
                    enabled = selectedDates != null,
                    onClick = {
                        val localTimeZone = ZoneId.systemDefault()

                        //Convert to local time
                        val from = selectedDates!!.start.atStartOfDay(localTimeZone).toInstant().toEpochMilli()
                        val to = selectedDates.endInclusive
                            //Add one day to include data for the last day in the selected range
                            .plusDays(1)
                            .atStartOfDay(localTimeZone)
                            .toInstant()
                            .toEpochMilli()

                        val workId = UUID.randomUUID()

                        WorkManager.getInstance(context).enqueue(
                            OneTimeWorkRequest.Builder(ReportSendWorker::class.java)
                                .setId(workId)
                                .setBackoffCriteria(BackoffPolicy.LINEAR, 30.seconds.toJavaDuration())
                                .setInputData(
                                    Data.Builder()
                                        .putLong(ReportSendWorker.INPUT_REUPLOAD_FROM, from)
                                        .putLong(ReportSendWorker.INPUT_REUPLOAD_TO, to)
                                        .build())
                                .setConstraints(Constraints.NONE)
                                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                .build()
                        )
                        enqueuedUploadWork.value = workId

                        dialogOpen.value = false
                    }
                ) {
                    Text(stringResource(id = R.string.reupload_data))
                }
            }) {
            Column {
                Text(
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp, 12.dp, 12.dp, 8.dp),
                    text = stringResource(id = R.string.reupload_data)
                )
                if (selectableDates.value == null) {
                    Box(
                        modifier = Modifier
                            .height(400.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.height(400.dp),
                        dateFormatter = DatePickerDefaults.dateFormatter(selectedDateSkeleton = "d/MM/yyyy"),
                        headline = {
                            //Wrap headline in a box to center the text on a single line
                            Box(
                                Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                DateRangePickerDefaults.DateRangePickerHeadline(
                                    selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                                    selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                                    displayMode = dateRangePickerState.displayMode,
                                    dateFormatter = DatePickerDefaults.dateFormatter(selectedDateSkeleton = "d/MM/yyyy"),
                                    modifier = Modifier.scale(0.9f)
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    Button(
        enabled = true,
        onClick = {
            dialogOpen.value = true
        }
    ) {
        Text(text = stringResource(id = R.string.reupload_data))
    }
}