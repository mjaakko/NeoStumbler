package xyz.malkki.neostumbler.ui.composables.settings.privacy

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isEditable
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys

class WifiFilterSettingsTest {
    private val testContext: Context = ApplicationProvider.getApplicationContext()

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testSavingWifiFilterSettings() = runTest {
        val settingsStore =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(coroutineContext + Dispatchers.IO),
                produceFile = { testContext.preferencesDataStoreFile("prefs") },
            )

        composeTestRule.setContent { WifiFilterSettings(settingsStore = settingsStore) }

        composeTestRule
            .onNodeWithText(testContext.getString(R.string.wifi_filter_settings_title))
            .performClick()

        composeTestRule.onNode(isDialog()).assertIsDisplayed()

        composeTestRule
            .onNode(isFocusable().and(isEditable()))
            .performTextInput(
                """
            ssid_1
            ssid_2
            
            ssid_3
        """
                    .trimIndent()
            )

        composeTestRule.onNodeWithText(testContext.getString(R.string.save)).performClick()

        await untilAsserted { composeTestRule.onNode(isDialog()).assertIsNotDisplayed() }

        val wifiFilterList =
            settingsStore.data
                .map { prefs -> prefs[stringSetPreferencesKey(PreferenceKeys.WIFI_FILTER_LIST)] }
                .firstOrNull()

        assertNotNull(wifiFilterList)
        assertEquals(setOf("ssid_1", "ssid_2", "ssid_3"), wifiFilterList)
    }
}
