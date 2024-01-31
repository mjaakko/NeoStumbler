package xyz.malkki.neostumbler.utils.charts

import com.patrykandpatrick.vico.core.chart.draw.ChartDrawContext
import com.patrykandpatrick.vico.core.chart.values.ChartValues
import com.patrykandpatrick.vico.core.chart.values.ChartValuesProvider
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MultiplesOfTenItemPlacerTest {
    @Test
    fun `Test generating labels with max value of 1432`() {
        val mockChartValues = mock<ChartValues>()
        whenever(mockChartValues.maxY).thenReturn(1432f)

        val mockChartValuesProvider = mock<ChartValuesProvider> {
            on { getChartValues(anyOrNull()) }.thenReturn(mockChartValues)
        }

        val mockContext = mock<ChartDrawContext>()
        whenever(mockContext.chartValuesProvider).thenReturn(mockChartValuesProvider)

        val itemPlacer = MultiplesOfTenItemPlacer(mock{})

        //Chart height 100, label height 10 -> max 10 labels -> step must be 200 to generate less than 10 labels
        val labels = itemPlacer.getLabelValues(mockContext, 100f, 10f, mock{})

        assertEquals(0.0f, labels.first(), 0.1f)
        assertEquals(1400.0f, labels.last(), 0.1f)
        assertEquals(8, labels.size)
    }
}