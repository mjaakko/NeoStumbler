package xyz.malkki.neostumbler.ichnaeaupload

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import xyz.malkki.neostumbler.core.someReport
import xyz.malkki.neostumbler.core.someWifiAccessPoint
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.reports.ReportSaver
import xyz.malkki.neostumbler.data.settings.FakeSettings
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.ichnaea.Geosubmit

class ReportSenderTest {
    private val reportTimestamp = Instant.now()!!

    private lateinit var geosubmit: Geosubmit
    private lateinit var reportSender: ReportSender

    private lateinit var settings: Settings

    @Before
    fun setup() {
        val reports = buildList {
            repeat(5) { id ->
                add(
                    someReport(
                        id = id.toLong(),
                        timestamp = reportTimestamp.plus(id.toLong(), ChronoUnit.DAYS),
                        wifiAccessPoints = listOf(someWifiAccessPoint()),
                    )
                )
            }
        }

        val reportProvider =
            mock<ReportProvider> {
                on { getNotUploadedReports(any()) } doReturnConsecutively
                    listOf(reports, emptyList())

                on { getRandomNotUploadedReports(any()) } doReturnConsecutively
                    listOf(reports.shuffled(), emptyList())

                on { getReportIdsForTimerange(any(), any()) } doReturnConsecutively
                    listOf(reports.map { it.id }, emptyList())

                on { getReportsByIds(any()) } doAnswer
                    {
                        val ids = it.arguments[0] as List<Long>

                        reports.filter { report -> report.id in ids }
                    }
            }

        geosubmit = mock<Geosubmit>()

        settings = FakeSettings()

        reportSender =
            ReportSender(
                geosubmit = geosubmit,
                reportProvider = reportProvider,
                reportSaver = mock<ReportSaver>(),
                settings = settings,
            )
    }

    @Test
    fun `Test sending reports`() = runTest {
        settings.edit { setBoolean(IchnaeaPreferenceKeys.REDUCED_METADATA, false) }

        reportSender.sendNotUploadedReports()

        argumentCaptor {
            verify(geosubmit, times(1)).sendReports(capture())

            val sentReports = firstValue

            assertEquals(5, sentReports.size)

            assertEquals(
                "01:01:01:01:01:01",
                sentReports.first().wifiAccessPoints?.firstOrNull()?.macAddress,
            )

            assertEquals(56.414156, sentReports.first().position.latitude, 0.0001)
            assertEquals(18.724728, sentReports.first().position.longitude, 0.0001)

            assertEquals(5.6378, sentReports.first().position.speed!!, 0.0001)

            assertEquals(reportTimestamp.toEpochMilli(), sentReports.first().timestamp)
        }
    }

    @Test
    fun `Test sending reports with reduced metadata`() = runTest {
        settings.edit { setBoolean(IchnaeaPreferenceKeys.REDUCED_METADATA, true) }

        reportSender.sendNotUploadedReports()

        argumentCaptor {
            verify(geosubmit, times(1)).sendReports(capture())

            val sentReports = firstValue

            assertEquals(5, sentReports.size)

            assertFalse(sentReports.map { it.timestamp }.isSorted())

            assertEquals(
                "01:01:01:01:01:01",
                sentReports.first().wifiAccessPoints?.firstOrNull()?.macAddress,
            )

            assertEquals(56.414156, sentReports.first().position.latitude, 0.0001)
            assertEquals(18.724728, sentReports.first().position.longitude, 0.0001)

            assertEquals(6.0, sentReports.first().position.speed!!, 0.0001)
            assertEquals(0.0, sentReports.first().position.heading!!, 0.0001)
        }
    }

    @Test
    fun `Test reuploading reports`() = runTest {
        settings.edit { setBoolean(IchnaeaPreferenceKeys.REDUCED_METADATA, false) }

        val from = Instant.now()!!.minusSeconds(3600)
        val to = Instant.now()

        reportSender.reuploadReports(from, to)

        argumentCaptor {
            verify(geosubmit, times(1)).sendReports(capture())

            val sentReports = firstValue

            assertEquals(5, sentReports.size)

            assertEquals(
                "01:01:01:01:01:01",
                sentReports.first().wifiAccessPoints?.firstOrNull()?.macAddress,
            )

            assertEquals(56.414156, sentReports.first().position.latitude, 0.0001)
            assertEquals(18.724728, sentReports.first().position.longitude, 0.0001)

            assertEquals(5.6378, sentReports.first().position.speed!!, 0.0001)
            assertEquals(14.516, sentReports.first().position.heading!!, 0.0001)

            assertEquals(reportTimestamp.toEpochMilli(), sentReports.first().timestamp)
        }
    }

    @Test
    fun `Test reuploading reports with reduced metadata`() = runTest {
        settings.edit { setBoolean(IchnaeaPreferenceKeys.REDUCED_METADATA, true) }

        val from = Instant.now()!!.minusSeconds(3600)
        val to = Instant.now()

        reportSender.reuploadReports(from, to)

        argumentCaptor {
            verify(geosubmit, times(1)).sendReports(capture())

            val sentReports = firstValue

            assertEquals(5, sentReports.size)

            assertFalse(sentReports.map { it.timestamp }.isSorted())

            assertEquals(
                "01:01:01:01:01:01",
                sentReports.first().wifiAccessPoints?.firstOrNull()?.macAddress,
            )

            assertEquals(56.414156, sentReports.first().position.latitude, 0.0001)
            assertEquals(18.724728, sentReports.first().position.longitude, 0.0001)

            assertEquals(6.0, sentReports.first().position.speed!!, 0.0001)
            assertEquals(0.0, sentReports.first().position.heading!!, 0.0001)
        }
    }
}
