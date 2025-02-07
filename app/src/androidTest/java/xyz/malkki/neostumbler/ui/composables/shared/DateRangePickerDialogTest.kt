package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DateRangePickerDialogTest {
    companion object {
        private val DATE_FORMAT =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.US)
    }

    @get:Rule val composeTestRule = createComposeRule()

    private var selectedDates: ClosedRange<LocalDate>? = null

    private lateinit var dates: Set<LocalDate>

    @Before
    fun setup() {
        dates = buildSet {
            var date = LocalDate.now()
            repeat(10) {
                add(date)

                date = date.plusDays(1)
            }
        }

        composeTestRule.setContent {
            DateRangePickerDialog(
                title = "test",
                selectButtonText = "select",
                selectableDates = mutableStateOf(dates),
                onDatesSelected = { selectedDates = it },
            )
        }
    }

    @Test
    fun testSelectingDateRange() {
        composeTestRule
            .onNode(hasText(dates.first().format(DATE_FORMAT), substring = true))
            .performClick()

        composeTestRule
            .onNode(hasText(dates.last().format(DATE_FORMAT), substring = true))
            .performClick()

        composeTestRule.onNodeWithText("select").performClick()

        assertEquals(dates.first(), selectedDates?.start)
        assertEquals(dates.last(), selectedDates?.endInclusive)
    }

    @Test
    fun testSelectIsDisabledWhenNothingSelected() {
        composeTestRule.onNodeWithText("select").assertIsNotEnabled()
    }
}
