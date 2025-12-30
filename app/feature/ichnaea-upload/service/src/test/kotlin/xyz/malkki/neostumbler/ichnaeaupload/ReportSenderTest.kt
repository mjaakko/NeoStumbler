package xyz.malkki.neostumbler.ichnaeaupload

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
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
import xyz.malkki.neostumbler.core.report.Report
import xyz.malkki.neostumbler.core.report.ReportEmitter
import xyz.malkki.neostumbler.core.report.ReportPosition
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.reports.ReportSaver
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.SettingsEditor
import xyz.malkki.neostumbler.data.settings.SettingsSnapshot
import xyz.malkki.neostumbler.ichnaea.Geosubmit

class ReportSenderTest {
    private val reportTimestamp = Instant.now()!!

    private lateinit var geosubmit: Geosubmit
    private lateinit var reportSender: ReportSender

    private lateinit var settings: Settings

    @Before
    fun setup() {
        val reports =
            listOf(
                Report(
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

        settings =
            object : Settings {
                private val settingsFlow = MutableStateFlow(FakeSettings(emptyMap()))

                override fun getSnapshotFlow(): Flow<SettingsSnapshot> {
                    return settingsFlow
                }

                override suspend fun edit(editSettings: SettingsEditor.() -> Unit) {
                    val updated = settingsFlow.value.values.toMutableMap()

                    object : SettingsEditor {
                            override fun setString(key: String, value: String) {
                                updated[key] = value
                            }

                            override fun setStringSet(key: String, value: Set<String>) {
                                updated[key] = value
                            }

                            override fun setBoolean(key: String, value: Boolean) {
                                updated[key] = value
                            }

                            override fun setInt(key: String, value: Int) {
                                updated[key] = value
                            }

                            override fun removeString(key: String) {
                                updated.remove(key)
                            }

                            override fun removeStringSet(key: String) {
                                updated.remove(key)
                            }

                            override fun removeBoolean(key: String) {
                                updated.remove(key)
                            }

                            override fun removeInt(key: String) {
                                updated.remove(key)
                            }
                        }
                        .editSettings()

                    settingsFlow.value = FakeSettings(updated)
                }
            }

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

            Assert.assertEquals(1, sentReports.size)

            Assert.assertEquals(
                "01:01:01:01:01:01",
                sentReports.first().wifiAccessPoints?.firstOrNull()?.macAddress,
            )

            Assert.assertEquals(56.414156, sentReports.first().position.latitude, 0.0001)
            Assert.assertEquals(18.724728, sentReports.first().position.longitude, 0.0001)

            Assert.assertEquals(5.6378, sentReports.first().position.speed!!, 0.0001)

            Assert.assertEquals(reportTimestamp.toEpochMilli(), sentReports.first().timestamp)
        }
    }

    @Test
    fun `Test sending reports with reduced metadata`() = runTest {
        settings.edit { setBoolean(IchnaeaPreferenceKeys.REDUCED_METADATA, true) }

        reportSender.sendNotUploadedReports()

        argumentCaptor {
            verify(geosubmit, times(1)).sendReports(capture())

            val sentReports = firstValue

            Assert.assertEquals(1, sentReports.size)

            Assert.assertEquals(
                "01:01:01:01:01:01",
                sentReports.first().wifiAccessPoints?.firstOrNull()?.macAddress,
            )

            Assert.assertEquals(56.414156, sentReports.first().position.latitude, 0.0001)
            Assert.assertEquals(18.724728, sentReports.first().position.longitude, 0.0001)

            Assert.assertEquals(6.0, sentReports.first().position.speed!!, 0.0001)
            Assert.assertEquals(0.0, sentReports.first().position.heading!!, 0.0001)

            Assert.assertEquals(
                reportTimestamp.truncatedTo(ChronoUnit.DAYS).toEpochMilli(),
                sentReports.first().timestamp,
            )
        }
    }

    class FakeSettings(val values: Map<String, Any>) : SettingsSnapshot {
        override fun getString(key: String): String? {
            return values[key] as String?
        }

        override fun getStringSet(key: String): Set<String>? {
            return values[key] as Set<String>?
        }

        override fun getBoolean(key: String): Boolean? {
            return values[key] as Boolean?
        }

        override fun getInt(key: String): Int? {
            return values[key] as Int?
        }
    }
}
