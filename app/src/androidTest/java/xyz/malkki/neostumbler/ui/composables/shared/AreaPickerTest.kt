package xyz.malkki.neostumbler.ui.composables.shared

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import java.io.IOException
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.Timeout
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinIsolatedContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.Position.Source
import xyz.malkki.neostumbler.data.location.LocationSource
import xyz.malkki.neostumbler.data.settings.DataStoreSettings
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.geography.LatLng

class AreaPickerTest {
    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

    private val mockHttpClient: Call.Factory =
        object : Call.Factory {
            override fun newCall(request: Request): Call {
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
                }
            }
        }

    private val testContext: Context = ApplicationProvider.getApplicationContext()

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testAreaPicker() = runTest {
        val fakeLocation =
            Position(
                latitude = 40.689100,
                longitude = -74.044300,
                source = Source.GPS,
                timestamp = 0,
            )

        val koin = koinApplication {
            modules(
                module {
                    androidContext(testContext)

                    single<Deferred<Call.Factory>> {
                        @OptIn(DelicateCoroutinesApi::class) GlobalScope.async { mockHttpClient }
                    }

                    single<DataStore<Preferences>>(PREFERENCES) {
                        PreferenceDataStoreFactory.create(
                            scope = CoroutineScope(coroutineContext + Dispatchers.IO),
                            produceFile = { testContext.preferencesDataStoreFile("prefs") },
                        )
                    }

                    single<Settings> { DataStoreSettings(get(PREFERENCES)) }

                    single<LocationSource> {
                        object : LocationSource {
                            override fun getLocations(
                                interval: Duration,
                                usePassiveProvider: Boolean,
                            ): Flow<Position> {
                                return flowOf(fakeLocation)
                            }
                        }
                    }
                }
            )
        }

        var selectedCircle: Pair<LatLng, Double>? = null

        composeTestRule.setContent {
            KoinIsolatedContext(koin) {
                AreaPickerDialog(
                    title = "Area picker",
                    positiveButtonText = "select",
                    onAreaSelected = { circle -> selectedCircle = circle },
                )
            }
        }

        await untilAsserted { composeTestRule.onNodeWithText("select").assertIsEnabled() }

        composeTestRule.onNodeWithText("select").performClick()

        assertNotNull(selectedCircle)
        assertEquals(40.689100, selectedCircle!!.first.latitude, 0.0001)
        assertEquals(-74.044300, selectedCircle!!.first.longitude, 0.0001)
        assertTrue(selectedCircle!!.second > 0)
    }
}
