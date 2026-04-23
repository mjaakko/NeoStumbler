package xyz.malkki.neostumbler.ichnaeaupload

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow

interface IchnaeaReportUploadStarter {
    val reportUploadEnqueued: Flow<Boolean>

    suspend fun awaitUntilUploaded(jobId: UUID): UploadResult?

    suspend fun startUploadNotUploadedReports(): UUID

    suspend fun startReuploadReports(from: Instant, to: Instant): UUID
}

sealed interface UploadResult {
    data class Success(val uploadedCount: Int) : UploadResult

    data class Failure(val type: FailureType, val errorMessage: String?) : UploadResult {
        enum class FailureType {
            NO_ENDPOINT,
            OTHER,
        }
    }
}
