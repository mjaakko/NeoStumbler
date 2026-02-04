package xyz.malkki.neostumbler.ichnaea

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.report.ReportEmitter
import xyz.malkki.neostumbler.core.report.ReportPosition
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.reports.ReportSaver

class ReportSenderTest {
    private val reportTimestamp = Instant.now()!!

    private lateinit var geosubmit: Geosubmit
    private lateinit var reportSender: ReportSender

    @Before
    fun setup() {
        val reports =
            listOf(
                xyz.malkki.neostumbler.core.report.Report(
                    id = 1,
                    timestamp = reportTimestamp,
                    uploaded = false,
                    uploadTimestamp = null,
                    position =
                        ReportPosition(
                            position =
                                Position(
                                    latitude = 56.414156,
                                    longitude = 18.724728,
                                    accuracy = 15.4,
                                    altitude = null,
                                    altitudeAccuracy = null,
                                    heading = 14.516,
                                    pressure = null,
                                    speed = 5.6378,
                                    source = Position.Source.GPS,
                                ),
                            age = 1000,
                        ),
                    wifiAccessPoints =
                        listOf(
                            ReportEmitter(
                                id = 1,
                                emitter =
                                    WifiAccessPoint(
                                        macAddress = MacAddress("01:01:01:01:01:01"),
                                        radioType = null,
                                        channel = null,
                                        frequency = null,
                                        signalStrength = null,
                                        ssid = "test_network",
                                    ),
                                age = 1500,
                            )
                        ),
                    cellTowers = emptyList(),
                    bluetoothBeacons = emptyList(),
                )
            )

        val reportProvider =
            mock<ReportProvider> {
                on { getNotUploadedReports(any()) } doReturnConsecutively
                    listOf(reports, emptyList())

                on { getRandomNotUploadedReports(any()) } doReturnConsecutively
                    listOf(reports, emptyList())
            }

        geosubmit = mock<Geosubmit>()

        reportSender =
            ReportSender(
                geosubmit = geosubmit,
                reportProvider = reportProvider,
                reportSaver = mock<ReportSaver>(),
            )
    }

    @Test
    fun `Test sending reports`() = runTest {
        reportSender.sendNotUploadedReports(reducedMetadata = false)

        argumentCaptor {
            verify(geosubmit, times(1)).sendReports(capture())

            val sentReports = firstValue

            assertEquals(1, sentReports.size)

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
        reportSender.sendNotUploadedReports(reducedMetadata = true)

        argumentCaptor {
            verify(geosubmit, times(1)).sendReports(capture())

            val sentReports = firstValue

            assertEquals(1, sentReports.size)

            assertEquals(
                "01:01:01:01:01:01",
                sentReports.first().wifiAccessPoints?.firstOrNull()?.macAddress,
            )

            assertEquals(56.414156, sentReports.first().position.latitude, 0.0001)
            assertEquals(18.724728, sentReports.first().position.longitude, 0.0001)

            assertEquals(6.0, sentReports.first().position.speed!!, 0.0001)
            assertEquals(0.0, sentReports.first().position.heading!!, 0.0001)

            assertEquals(
                reportTimestamp.truncatedTo(ChronoUnit.DAYS).toEpochMilli(),
                sentReports.first().timestamp,
            )
        }
    }
}
