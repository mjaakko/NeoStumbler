package xyz.malkki.neostumbler.ui.composables.reports

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.UUID
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ichnaeaupload.IchnaeaReportUploadStarter
import xyz.malkki.neostumbler.ichnaeaupload.UploadResult

@Composable
fun ToastOnReportUpload(
    workId: MutableState<UUID?>,
    ichnaeaReportUploadStarter: IchnaeaReportUploadStarter = koinInject(),
) {
    val context = LocalContext.current

    LaunchedEffect(workId.value) {
        if (workId.value != null) {
            val result = ichnaeaReportUploadStarter.awaitUntilUploaded(workId.value!!)

            if (result is UploadResult.Success) {
                context.showToast(
                    context.getQuantityString(
                        R.plurals.toast_reports_uploaded,
                        result.uploadedCount,
                        result.uploadedCount,
                    )
                )
            } else if (result is UploadResult.Failure) {
                when (result.type) {
                    UploadResult.Failure.FailureType.NO_ENDPOINT -> {
                        context.showToast(
                            ContextCompat.getString(
                                context,
                                R.string.toast_reports_upload_failed_no_endpoint,
                            )
                        )
                    }
                    UploadResult.Failure.FailureType.OTHER -> {
                        val toastText = buildString {
                            append(
                                ContextCompat.getString(
                                    context,
                                    R.string.toast_reports_upload_failed,
                                )
                            )

                            if (result.errorMessage != null) {
                                append("\n\n")
                                append(result.errorMessage)
                            }
                        }
                        context.showToast(toastText, length = Toast.LENGTH_LONG)
                    }
                }
            }

            workId.value = null
        }
    }
}
