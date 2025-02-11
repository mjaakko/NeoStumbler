package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class UrlFieldTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testWarningIsShownForInvalidUrls() {
        val state = mutableStateOf<String?>("https://example.com")

        composeTestRule.setContent { UrlField(label = "url", state = remember { state }) }

        composeTestRule.onNodeWithText("Invalid URL").assertIsNotDisplayed()

        state.value = "not a valid url"

        composeTestRule.onNodeWithText("Invalid URL").assertIsDisplayed()
    }
}
