package xyz.malkki.neostumbler.ui.composables.troubleshooting

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class TroubleshootingItemTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testTroubleshootingItem() {
        val state = MutableStateFlow(false)

        composeTestRule.setContent {
            TroubleshootingItem(
                title = "problem",
                stateFlow = state,
                fixAction = { state.value = true },
            )
        }

        composeTestRule.onAllNodesWithContentDescription("Not ok").assertCountEquals(1)

        composeTestRule.onNodeWithText("Fix").performClick()

        composeTestRule.onAllNodesWithContentDescription("Ok").assertCountEquals(1)
    }
}
