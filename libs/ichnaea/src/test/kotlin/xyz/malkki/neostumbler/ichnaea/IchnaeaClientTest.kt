package xyz.malkki.neostumbler.ichnaea

import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import xyz.malkki.neostumbler.ichnaea.dto.BluetoothBeaconDto
import xyz.malkki.neostumbler.ichnaea.dto.CellTowerDto
import xyz.malkki.neostumbler.ichnaea.dto.GeolocateRequestDto
import xyz.malkki.neostumbler.ichnaea.dto.ReportDto

class IchnaeaClientTest {
    @get:Rule private val mockServer = MockWebServer()

    private val ichnaeaClient: IchnaeaClient =
        IchnaeaClient(
            httpClient = OkHttpClient(),
            ichnaeaParams =
                IchnaeaParams(
                    baseUrl = mockServer.url("/").toString(),
                    submissionPath = "/v2/geosubmit",
                    locatePath = "/v1/geolocate",
                    apiKey = null,
                ),
        )

    @Test
    fun `Test sending a report`() = runTest {
        mockServer.enqueue(MockResponse().setBody("{}"))

        val reports =
            listOf(
                ReportDto(
                    timestamp = 0L,
                    position =
                        ReportDto.PositionDto(
                            latitude = 56.3612,
                            longitude = 12.5166,
                            accuracy = 100.0,
                            age = 100L,
                            altitude = 56.5,
                            altitudeAccuracy = 5.0,
                            heading = 15.6,
                            pressure = 1013.25,
                            speed = 3.4,
                            source = "gps",
                        ),
                    wifiAccessPoints = null,
                    cellTowers = null,
                    bluetoothBeacons =
                        listOf(
                            BluetoothBeaconDto(
                                macAddress = "01:01:01:01:01:01",
                                age = 100,
                                signalStrength = -70,
                            )
                        ),
                )
            )

        ichnaeaClient.sendReports(reports)

        val request = withTimeout(1.seconds) { runInterruptible { mockServer.takeRequest() } }

        val requestBody =
            GZIPInputStream(request.body.inputStream()).use {
                it.readAllBytes().toString(StandardCharsets.UTF_8)
            }
        assertTrue(requestBody.contains("01:01:01:01:01:01"))

        // Check that request body does not contain nulls -> Ichnaea servers don't accept them
        assertFalse(requestBody.contains("null"))
    }

    @Test
    fun `Test receiving a geolocate response with extra values`() = runTest {
        mockServer.enqueue(
            MockResponse().apply {
                setBody(
                    """
                    {
                      "license": "test license",
                      "location": {
                        "lat": 50.42352,
                        "lng": 12.32135
                      },
                      "accuracy": 25000,
                      "fallback": "ipf"
                    }
                    """
                        .trimIndent()
                )
            }
        )

        val response =
            ichnaeaClient.getLocation(
                GeolocateRequestDto(
                    considerIp = true,
                    cellTowers =
                        listOf(
                            CellTowerDto(
                                radioType = "lte",
                                mobileCountryCode = 10,
                                mobileNetworkCode = 100,
                                locationAreaCode = 255,
                                cellId = 123456,
                                signalStrength = -78,
                            )
                        ),
                    bluetoothBeacons = emptyList(),
                    wifiAccessPoints = emptyList(),
                )
            )

        assertEquals(50.42352, response.location.lat, 0.0001)
        assertEquals(12.32135, response.location.lng, 0.0001)
    }
}
