package xyz.malkki.neostumbler.ui.composables.troubleshooting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class TroubleshootingItemTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testTroubleshootingItem() {
        var isOk by mutableStateOf(false)

        composeTestRule.setContent {
            TroubleshootingItem(
                title = "problem",
                isOk = isOk,
                fixAction = { isOk = true },
            )
        }

        composeTestRule.onAllNodesWithContentDescription("Not ok").assertCountEquals(1)

        composeTestRule.onNodeWithText("Fix").performClick()

        composeTestRule.onAllNodesWithContentDescription("Ok").assertCountEquals(1)
    }
}
