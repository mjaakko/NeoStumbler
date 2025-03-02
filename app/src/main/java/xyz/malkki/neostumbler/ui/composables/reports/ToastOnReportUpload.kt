package xyz.malkki.neostumbler.ui.composables.reports

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.UUID
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ichnaea.ReportSendWorker
import xyz.malkki.neostumbler.ui.composables.shared.EffectOnWorkCompleted

@Composable
fun ToastOnReportUpload(workId: MutableState<UUID?>) {
    val context = LocalContext.current

    EffectOnWorkCompleted(
        workId = workId.value,
        onWorkSuccess = { workInfo ->
            val reportsUploaded =
                workInfo.outputData.getInt(ReportSendWorker.OUTPUT_REPORTS_SENT, 0)
            context.showToast(
                context.getQuantityString(
                    R.plurals.toast_reports_uploaded,
                    reportsUploaded,
                    reportsUploaded,
                )
            )

            workId.value = null
        },
        onWorkFailed = { workInfo ->
            val errorType = workInfo.outputData.getInt(ReportSendWorker.OUTPUT_ERROR_TYPE, -1)

            when (errorType) {
                ReportSendWorker.ERROR_TYPE_NO_ENDPOINT_CONFIGURED -> {
                    context.showToast(
                        ContextCompat.getString(
                            context,
                            R.string.toast_reports_upload_failed_no_endpoint,
                        )
                    )
                }

                else -> {
                    val errorMessage =
                        workInfo.outputData.getString(ReportSendWorker.OUTPUT_ERROR_MESSAGE)

                    val toastText = buildString {
                        append(
                            ContextCompat.getString(context, R.string.toast_reports_upload_failed)
                        )

                        if (errorMessage != null) {
                            append("\n\n")
                            append(errorMessage)
                        }
                    }
                    context.showToast(toastText, length = Toast.LENGTH_LONG)
                }
            }

            workId.value = null
        },
    )
}
