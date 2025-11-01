package xyz.malkki.neostumbler.ui.composables.reports.details

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import java.time.Instant
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.Timeout
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.Rule
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinIsolatedContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.report.ReportEmitter
import xyz.malkki.neostumbler.core.report.ReportPosition
import xyz.malkki.neostumbler.data.settings.DataStoreSettings
import xyz.malkki.neostumbler.data.settings.Settings

class ReportMapTest {
    private val requests: MutableList<Request> = mutableListOf()

    private val mockHttpClient: Call.Factory =
        object : Call.Factory {
            override fun newCall(request: Request): Call {
                requests += request

                return object : Call {
                    override fun cancel() {}

                    override fun clone(): Call {
                        return this
                    }

                    override fun enqueue(responseCallback: Callback) {
                        responseCallback.onFailure(this, IOException("Failed"))
                    }

                    override fun execute(): Response {
                        throw IOException("Failed")
                    }

                    override fun isCanceled(): Boolean {
                        return false
                    }

                    override fun isExecuted(): Boolean {
                        return false
                    }

                    override fun request(): Request {
                        return request
                    }

                    override fun timeout(): Timeout {
                        return timeout()
                    }

                    override fun <T : Any> tag(type: KClass<T>): T? {
                        return null
                    }

                    override fun <T> tag(type: Class<out T>): T? {
                        return null
                    }

                    override fun <T : Any> tag(type: KClass<T>, computeIfAbsent: () -> T): T {
                        return computeIfAbsent()
                    }

                    override fun <T : Any> tag(type: Class<T>, computeIfAbsent: () -> T): T {
                        return computeIfAbsent()
                    }
                }
            }
        }

    private val testContext: Context = ApplicationProvider.getApplicationContext()

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testReportMapQueriesLocationFromConfiguredEndpoint() = runTest {
        val settingsStore =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(coroutineContext + Dispatchers.IO),
                produceFile = { testContext.preferencesDataStoreFile("prefs") },
            )

        settingsStore.edit { prefs ->
            prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_ENDPOINT)] = "http://example.com"
            prefs[stringPreferencesKey(PreferenceKeys.GEOSUBMIT_PATH)] = "/test"
            prefs[stringPreferencesKey(PreferenceKeys.GEOLOCATE_PATH)] = "/geolocate"
        }

        val koin = koinApplication {
            modules(
                module {
                    androidContext(testContext)

                    single<Deferred<Call.Factory>> {
                        @OptIn(DelicateCoroutinesApi::class) GlobalScope.async { mockHttpClient }
                    }

                    single<DataStore<Preferences>>(PREFERENCES) { settingsStore }

                    single<Settings> { DataStoreSettings(get(PREFERENCES)) }
                }
            )
        }

        composeTestRule.setContent {
            KoinIsolatedContext(koin) {
                ReportMap(
                    report =
                        xyz.malkki.neostumbler.core.report.Report(
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
                                    age = 1000,
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
                                        age = 1000,
                                    )
                                ),
                            bluetoothBeacons = emptyList(),
                        ),
                    modifier = Modifier.size(300.dp),
                )
            }
        }

        await untilAsserted
            {
                assertTrue(requests.isNotEmpty())

                val requestUrl = requests.first().url

                assertEquals("example.com", requestUrl.host)
                assertEquals("/geolocate", requestUrl.encodedPath)
            }
    }
}
