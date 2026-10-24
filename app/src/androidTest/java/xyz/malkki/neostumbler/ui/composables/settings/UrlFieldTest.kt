package xyz.malkki.neostumbler.ui.composables.settings

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isEditable
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class UrlFieldTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testWarningIsShownForInvalidUrls() {
        val textFieldState = TextFieldState("https://example.com")

        composeTestRule.setContent {
            UrlField(modifier = Modifier, label = "url", state = textFieldState)
        }

        composeTestRule.onNodeWithText("Invalid URL").assertIsNotDisplayed()

        composeTestRule.onNode(isFocusable().and(isEditable())).performTextInput("not a valid url")

        composeTestRule.onNodeWithText("Invalid URL").assertIsDisplayed()
    }
}
