package xyz.malkki.neostumbler.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import xyz.malkki.neostumbler.ui.viewmodel.StatisticsViewModel
import xyz.malkki.neostumbler.utils.charts.MultiplesOfTenItemPlacer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

@Composable
private fun StationsByDayChart(entryModel: ChartEntryModelProducer, title: String) {
    Text(title)
    Chart(
        chart = lineChart(),
        chartModelProducer = entryModel,
        startAxis = rememberStartAxis(itemPlacer = remember { MultiplesOfTenItemPlacer() }, valueFormatter = { value, _ ->
            value.toLong().toString()
        }),
        bottomAxis = rememberBottomAxis(valueFormatter = { value, _ ->
            LocalDate.ofEpochDay(value.toLong()).format(dateTimeFormatter)
        }),
        modifier = Modifier.height(320.dp)
    )
}
@Composable
fun StatisticsScreen(statisticsViewModel: StatisticsViewModel = viewModel()) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StationsByDayChart(entryModel = statisticsViewModel.wifiEntryModel, title = "Wi-Fis")
        Spacer(modifier = Modifier.height(16.dp))
        StationsByDayChart(entryModel = statisticsViewModel.cellEntryModel, title = "Cells")
        Spacer(modifier = Modifier.height(16.dp))
        StationsByDayChart(entryModel = statisticsViewModel.beaconEntryModel, title = "Beacons")
        Spacer(modifier = Modifier.height(16.dp))
    }
}