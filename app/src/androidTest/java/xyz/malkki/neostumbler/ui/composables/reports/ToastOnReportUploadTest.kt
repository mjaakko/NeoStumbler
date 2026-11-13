package xyz.malkki.neostumbler.ui.composables.reports

import androidx.compose.ui.test.junit4.v2.createComposeRule
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import xyz.malkki.neostumbler.ichnaeaupload.IchnaeaReportUploadStarter
import xyz.malkki.neostumbler.ichnaeaupload.UploadResult

class ToastOnReportUploadTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testOnToastShownCalledOnSuccess() = runTest {
        val testWorkId = UUID.randomUUID()
        var onToastShownCalled = false

        val fakeUploadStarter =
            object : IchnaeaReportUploadStarter {
                override val reportUploadEnqueued = flowOf(value = false)

                override suspend fun awaitUntilUploaded(jobId: UUID): UploadResult? {
                    if (jobId == testWorkId) {
                        return UploadResult.Success(uploadedCount = 5)
                    }
                    return null
                }

                override suspend fun startUploadNotUploadedReports(): UUID {
                    return UUID.randomUUID()
                }

                override suspend fun startReuploadReports(
                    from: Instant,
                    to: Instant,
                ): UUID {
                    return UUID.randomUUID()
                }
            }

        composeTestRule.setContent {
            ToastOnReportUpload(
                workId = testWorkId,
                onToastShown = { onToastShownCalled = true },
                ichnaeaReportUploadStarter = fakeUploadStarter,
            )
        }

        composeTestRule.awaitIdle()

        assertTrue("onToastShown should have been called", onToastShownCalled)
    }
}
