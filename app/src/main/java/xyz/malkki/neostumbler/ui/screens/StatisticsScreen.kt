package xyz.malkki.neostumbler.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.defaultLocale
import xyz.malkki.neostumbler.ui.viewmodel.StatisticsViewModel
import xyz.malkki.neostumbler.utils.charts.MultiplesOfTenItemPlacer
import xyz.malkki.neostumbler.utils.charts.TextLabelItemPlacer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
private fun StationsByDayChart(entryModel: ChartEntryModelProducer, title: String) {
    val dateFormatPattern = DateFormat.getBestDateTimePattern(LocalContext.current.defaultLocale, "d MMM")
    val dateFormat = DateTimeFormatter.ofPattern(dateFormatPattern)

    Text(title)
    Chart(
        chart = lineChart(),
        chartModelProducer = entryModel,
        startAxis = rememberStartAxis(
            itemPlacer = remember { MultiplesOfTenItemPlacer() },
            valueFormatter = { value, _ ->
                value.toLong().toString()
            }
        ),
        bottomAxis = rememberBottomAxis(
            valueFormatter = { value, _ ->
                LocalDate.ofEpochDay(value.toLong()).format(dateFormat)
            },
            itemPlacer = remember { TextLabelItemPlacer() }
        ),
        chartScrollSpec = rememberChartScrollSpec(initialScroll = InitialScroll.End),
        modifier = Modifier.height(320.dp)
    )
}
@Composable
fun StatisticsScreen(statisticsViewModel: StatisticsViewModel = viewModel()) {
    val dataLoaded = statisticsViewModel.dataLoaded.observeAsState(initial = false)

    if (!dataLoaded.value) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            StationsByDayChart(entryModel = statisticsViewModel.wifiEntryModel, title = stringResource(id = R.string.wifis))
            Spacer(modifier = Modifier.height(16.dp))
            StationsByDayChart(entryModel = statisticsViewModel.cellEntryModel, title = stringResource(id = R.string.cells))
            Spacer(modifier = Modifier.height(16.dp))
            StationsByDayChart(entryModel = statisticsViewModel.beaconEntryModel, title = stringResource(id = R.string.beacons))
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}