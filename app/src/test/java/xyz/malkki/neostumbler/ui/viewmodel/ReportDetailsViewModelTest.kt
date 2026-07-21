package xyz.malkki.neostumbler.ui.viewmodel

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.PreferencesFileSerializer
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.report.Report
import xyz.malkki.neostumbler.core.report.ReportEmitter
import xyz.malkki.neostumbler.core.report.ReportPosition
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.settings.DataStoreSettings
import xyz.malkki.neostumbler.ichnaea.IchnaeaParams
import xyz.malkki.neostumbler.ichnaeaupload.IchnaeaClientProvider

private val REPORT =
    Report(
        id = 1,
        timestamp = Instant.now(),
        uploaded = false,
        uploadTimestamp = null,
        position =
            ReportPosition(
                position =
                    Position(
                        latitude = 53.3677,
                        longitude = 42.141656,
                        accuracy = 10.0,
                        altitude = null,
                        altitudeAccuracy = null,
                        heading = null,
                        pressure = null,
                        speed = null,
                        source = Position.Source.GPS,
                    ),
                age = 1000.milliseconds,
            ),
        wifiAccessPoints = emptyList(),
        cellTowers =
            listOf(
                ReportEmitter(
                    id = 1,
                    emitter =
                        CellTower(
                            radioType = CellTower.RadioType.LTE,
                            mobileCountryCode = "1",
                            mobileNetworkCode = "1",
                            cellId = 321,
                            locationAreaCode = 555,
                            asu = null,
                            primaryScramblingCode = null,
                            serving = null,
                            signalStrength = null,
                            timingAdvance = null,
                            arfcn = null,
                        ),
                    age = 1000.milliseconds,
                )
            ),
        bluetoothBeacons = emptyList(),
    )

class ReportDetailsViewModelTest {
    @get:Rule val tmpFolder = TemporaryFolder()

    private lateinit var testScope: TestScope

    private lateinit var mockServer: MockWebServer

    private lateinit var viewModel: ReportDetailsViewModel

    @Before
    fun setup() {
        testScope = TestScope(UnconfinedTestDispatcher() + Job())

        mockServer = MockWebServer()
        mockServer.enqueue(
            MockResponse()
                .setBody(
                    """
                    { "location": {"lat": 0.0, "lng": 0.0}, "accuracy": 500.0 }
                    """
                        .trimIndent()
                )
        )

        mockServer.start()

        val settingsStore =
            DataStoreFactory.create(
                serializer = PreferencesFileSerializer,
                produceFile = { tmpFolder.newFile("prefs.pb") },
                scope = testScope,
            )

        val ichnaeaClientProvider =
            IchnaeaClientProvider(
                httpClientProvider = { OkHttpClient() },
                settings = DataStoreSettings(settingsStore),
            )

        viewModel =
            ReportDetailsViewModel(
                reportId = 1,
                reportProvider =
                    mock<ReportProvider> { on { getReport(any()) } doReturn flowOf(REPORT) },
                ichnaeaClientProvider = ichnaeaClientProvider,
            )

        runBlocking {
            ichnaeaClientProvider.setIchnaeaParams(
                IchnaeaParams(
                    baseUrl = mockServer.url("/").toString(),
                    submissionPath = "/geosubmit",
                    locatePath = "/geolocate",
                    apiKey = null,
                )
            )
        }
    }

    @After
    fun shutdown() {
        mockServer.shutdown()
    }

    @Test
    fun `Report location is queried from the configured endpoint`() = testScope.runTest {
        assertNotNull(viewModel.estimatedReportLocation.filterNotNull().firstOrNull())

        val request = runInterruptible { mockServer.takeRequest() }
        val requestBody = request.body.use { it.readString(StandardCharsets.UTF_8) }

        assertTrue("\"cellId\":321" in requestBody)
    }
}
