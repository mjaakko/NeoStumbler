package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConfirmationDialogTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testPositiveAction() {
        var positive = 0
        var negative = 0

        val positiveAction = { positive = positive + 1 }
        val negativeAction = { negative = negative + 1 }

        composeTestRule.setContent {
            ConfirmationDialog(
                title = "title",
                description = "description",
                positiveButtonText = "yes",
                negativeButtonText = "no",
                onPositiveAction = positiveAction,
                onNegativeAction = negativeAction,
            )
        }

        composeTestRule.onNodeWithText("yes").performClick()

        assertEquals(positive, 1)
        assertEquals(negative, 0)
    }

    @Test
    fun testNegativeAction() {
        var positive = 0
        var negative = 0

        val positiveAction = { positive = positive + 1 }
        val negativeAction = { negative = negative + 1 }

        composeTestRule.setContent {
            ConfirmationDialog(
                title = "title",
                description = "description",
                positiveButtonText = "yes",
                negativeButtonText = "no",
                onPositiveAction = positiveAction,
                onNegativeAction = negativeAction,
            )
        }

        composeTestRule.onNodeWithText("no").performClick()

        assertEquals(positive, 0)
        assertEquals(negative, 1)
    }
}
