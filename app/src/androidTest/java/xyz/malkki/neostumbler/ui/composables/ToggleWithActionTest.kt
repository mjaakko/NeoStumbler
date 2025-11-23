package xyz.malkki.neostumbler.ui.composables

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ToggleWithActionTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testDisabledToggle() = runTest {
        var checked = false

        composeTestRule.setContent {
            ToggleWithAction(
                title = "test_title",
                description = "test_description",
                warningWhenDisabled = "test_warning",
                enabled = false,
                checked = checked,
                action = { checked = it },
            )
        }

        composeTestRule.onNodeWithText("test_warning").assertIsDisplayed()

        composeTestRule.onRoot().performClick()

        composeTestRule.awaitIdle()

        assertFalse(checked)
    }

    @Test
    fun testEnabledToggle() = runTest {
        var checked = false

        composeTestRule.setContent {
            ToggleWithAction(
                title = "test_title",
                description = "test_description",
                warningWhenDisabled = "test_warning",
                enabled = true,
                checked = checked,
                action = { checked = it },
            )
        }

        composeTestRule.onNodeWithText("test_warning").assertIsNotDisplayed()

        composeTestRule.onRoot().performClick()

        composeTestRule.awaitIdle()

        assertTrue(checked)
    }
}
