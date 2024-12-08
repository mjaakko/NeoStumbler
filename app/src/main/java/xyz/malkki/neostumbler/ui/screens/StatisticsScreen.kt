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
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.defaultLocale
import xyz.malkki.neostumbler.ui.viewmodel.StatisticsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

@Composable
private fun StationsByDayChart(entryModel: CartesianChartModelProducer, title: String) {
    val dateFormatPattern = DateFormat.getBestDateTimePattern(LocalContext.current.defaultLocale, "d MMM")
    val dateFormat = DateTimeFormatter.ofPattern(dateFormatPattern)

    Text(title)
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(
                itemPlacer = remember {
                    VerticalAxis.ItemPlacer.step(step = { extras ->
                        val max = extras[StatisticsViewModel.MAX_Y_VALUE_KEY]

                        10.0.pow(floor(log10(max.toDouble()))) / 10
                    })
                }
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = remember {
                    CartesianValueFormatter { context, value, pos ->
                        LocalDate.ofEpochDay(value.toLong()).format(dateFormat)
                    }
                }
            ),
        ),
        modelProducer = entryModel,
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
            ProvideVicoTheme(rememberM3VicoTheme()) {
                StationsByDayChart(entryModel = statisticsViewModel.wifiEntryModel, title = stringResource(id = R.string.wifis))
                Spacer(modifier = Modifier.height(16.dp))
                StationsByDayChart(entryModel = statisticsViewModel.cellEntryModel, title = stringResource(id = R.string.cells))
                Spacer(modifier = Modifier.height(16.dp))
                StationsByDayChart(entryModel = statisticsViewModel.beaconEntryModel, title = stringResource(id = R.string.beacons))
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}