package xyz.malkki.neostumbler.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import xyz.malkki.neostumbler.StumblerApplication
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

@Composable
private fun StationsByDayChart(dataSource: LiveData<Map<LocalDate, Long>>, title: String) {
    val chartEntryModelProducer = ChartEntryModelProducer(emptyList<ChartEntry>())
    dataSource.observe(LocalLifecycleOwner.current) { wifisPerDay ->
        val entries = wifisPerDay.map { (date, count) ->
            entryOf(date.toEpochDay(), count)
        }
        chartEntryModelProducer.setEntries(entries)
    }

    Text(title)
    Chart(
        chart = columnChart(),
        chartModelProducer = chartEntryModelProducer,
        startAxis = rememberStartAxis(valueFormatter = { value, _ ->
            value.toLong().toString()
        }),
        bottomAxis = rememberBottomAxis(valueFormatter = { value, _ ->
            LocalDate.ofEpochDay(value.toLong()).format(dateTimeFormatter)
        }),
        modifier = Modifier.height(320.dp)
    )
}

@Composable
fun StatisticsScreen() {
    val context = LocalContext.current

    val statisticsDao = (context.applicationContext as StumblerApplication).reportDb.statisticsDao()

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StationsByDayChart(dataSource = statisticsDao.newWifisPerDay(), title = "Wi-Fis")
        Spacer(modifier = Modifier.height(16.dp))
        StationsByDayChart(dataSource = statisticsDao.newCellsPerDay(), title = "Cells")
        Spacer(modifier = Modifier.height(16.dp))
        StationsByDayChart(dataSource = statisticsDao.newBeaconsPerDay(), title = "Beacons")
        Spacer(modifier = Modifier.height(16.dp))
    }
}
