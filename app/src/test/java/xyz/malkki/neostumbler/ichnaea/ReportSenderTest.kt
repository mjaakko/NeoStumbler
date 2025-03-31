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
import xyz.malkki.neostumbler.db.dao.ReportDao
import xyz.malkki.neostumbler.db.entities.PositionEntity
import xyz.malkki.neostumbler.db.entities.Report
import xyz.malkki.neostumbler.db.entities.ReportWithData
import xyz.malkki.neostumbler.db.entities.WifiAccessPointEntity

class ReportSenderTest {
    private val reportTimestamp = Instant.now()!!

    private lateinit var geosubmit: Geosubmit
    private lateinit var reportSender: ReportSender

    @Before
    fun setup() {
        val reports =
            listOf(
                ReportWithData(
                    report =
                        Report(
                            id = 1,
                            timestamp = reportTimestamp,
                            uploaded = false,
                            uploadTimestamp = null,
                        ),
                    positionEntity =
                        PositionEntity(
                            id = 1,
                            latitude = 56.414156,
                            longitude = 18.724728,
                            accuracy = 15.4,
                            age = 1000,
                            altitude = null,
                            altitudeAccuracy = null,
                            heading = 14.516,
                            pressure = null,
                            speed = 5.6378,
                            source = "gps",
                            reportId = 1,
                        ),
                    wifiAccessPointEntities =
                        listOf(
                            WifiAccessPointEntity(
                                id = 1,
                                macAddress = "01:01:01:01:01:01",
                                radioType = null,
                                age = 1500,
                                channel = null,
                                frequency = null,
                                signalStrength = null,
                                signalToNoiseRatio = null,
                                ssid = "test_network",
                                reportId = 1,
                            )
                        ),
                    cellTowerEntities = emptyList(),
                    bluetoothBeaconEntities = emptyList(),
                )
            )

        val reportDao =
            mock<ReportDao> {
                onBlocking { getNotUploadedReports(any()) } doReturnConsecutively
                    listOf(reports, emptyList())

                onBlocking { getRandomNotUploadedReports(any()) } doReturnConsecutively
                    listOf(reports, emptyList())
            }

        geosubmit = mock<Geosubmit>()

        reportSender = ReportSender(geosubmit, reportDao)
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
