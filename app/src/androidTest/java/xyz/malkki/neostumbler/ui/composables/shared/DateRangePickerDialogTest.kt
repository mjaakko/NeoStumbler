package xyz.malkki.neostumbler.ui.composables.shared

import android.icu.text.DateFormat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import xyz.malkki.neostumbler.extensions.defaultLocale

class DateRangePickerDialogTest {
    @get:Rule val composeTestRule = createComposeRule()

    private var selectedDates: ClosedRange<LocalDate>? = null

    private lateinit var dates: Set<LocalDate>

    private lateinit var dateFormat: DateFormat

    @Before
    fun setup() {
        dates = buildSet {
            var date = LocalDate.now()
            repeat(10) {
                add(date)

                date = date.plusDays(1)
            }
        }

        val locale = InstrumentationRegistry.getInstrumentation().targetContext.defaultLocale
        dateFormat = DateFormat.getInstanceForSkeleton(DateFormat.YEAR_MONTH_DAY, locale)

        composeTestRule.setContent {
            DateRangePickerDialog(
                title = "test",
                selectButtonText = "select",
                selectableDates = remember { mutableStateOf(dates) },
                onDatesSelected = { selectedDates = it },
            )
        }
    }

    @Test
    fun testSelectingDateRange() {
        val startDate =
            composeTestRule.onNode(
                hasText(" " + dates.first().format(dateFormat), substring = true)
            )

        if (startDate.isNotDisplayed()) {
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeUp(startY = centerY, endY = top)
            }
        }

        startDate.performClick()

        val endDate =
            composeTestRule.onNode(hasText(" " + dates.last().format(dateFormat), substring = true))

        // If the end date is not visible, scroll the calendar to show the next month
        if (endDate.isNotDisplayed()) {
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeUp(startY = centerY, endY = top)
            }
        }

        endDate.performClick()

        composeTestRule.onNodeWithText("select").performClick()

        assertEquals(dates.first(), selectedDates?.start)
        assertEquals(dates.last(), selectedDates?.endInclusive)
    }

    @Test
    fun testSelectIsDisabledWhenNothingSelected() {
        composeTestRule.onNodeWithText("select").assertIsNotEnabled()
    }

    private fun LocalDate.format(dateFormat: DateFormat): String {
        return dateFormat.format(Date.from(atStartOfDay(ZoneId.of("UTC")).toInstant()))
    }
}
