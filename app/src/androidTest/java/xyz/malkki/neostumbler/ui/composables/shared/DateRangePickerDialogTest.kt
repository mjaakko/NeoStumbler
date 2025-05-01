package xyz.malkki.neostumbler.ui.composables.shared

import android.icu.text.DateFormat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
            var date = LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(11)
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
        val startDateMatcher = hasText(" " + dates.first().format(dateFormat), substring = true)

        val startDate = composeTestRule.onNode(startDateMatcher)

        startDate.performScrollTo()
        startDate.performClick()
        startDate.assertIsSelected()

        val endDateMatcher = hasText(" " + dates.last().format(dateFormat), substring = true)

        val endDate = composeTestRule.onNode(endDateMatcher)

        endDate.performScrollTo()
        endDate.performClick()
        endDate.assertIsSelected()

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
