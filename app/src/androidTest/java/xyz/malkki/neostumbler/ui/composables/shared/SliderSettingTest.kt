package xyz.malkki.neostumbler.ui.composables.shared

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasNoClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import xyz.malkki.neostumbler.data.settings.DataStoreSettings
import xyz.malkki.neostumbler.ui.composables.settings.SliderSetting

class SliderSettingTest {
    private val testContext: Context = ApplicationProvider.getApplicationContext()

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testSliderShowsCorrectValues() = runTest {
        val range = 0..50
        val step = 5

        val settingsStore =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(coroutineContext + Dispatchers.IO),
                produceFile = { testContext.preferencesDataStoreFile("prefs") },
            )
        composeTestRule.setContent {
            SliderSetting(
                title = "slider test",
                preferenceKey = "slider_test",
                range = range,
                step = step,
                default = 0,
                valueFormatter = { "value: $it" },
                settings = DataStoreSettings(settingsStore),
                saveButtonText = "save",
            )
        }

        composeTestRule
            .onNode(hasAnyChild(hasText("slider test")).and(hasAnyChild(hasText("value: 0"))))
            .performClick()

        var value = 0

        while (
            composeTestRule
                .onNode(hasText("value: ${range.endInclusive}").and(hasNoClickAction()))
                .isNotDisplayed()
        ) {
            composeTestRule
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
                .performSemanticsAction(SemanticsActions.SetProgress) { it.invoke(value.toFloat()) }

            value += step
        }

        composeTestRule.onNodeWithText("save").performClick()

        composeTestRule.waitUntil { composeTestRule.onNode(isDialog()).isNotDisplayed() }

        assertEquals(
            50,
            settingsStore.data.map { it[intPreferencesKey("slider_test")] }.firstOrNull(),
        )
    }
}
