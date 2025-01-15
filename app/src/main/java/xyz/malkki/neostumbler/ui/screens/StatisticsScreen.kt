package xyz.malkki.neostumbler.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.Zoom
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
private fun StationsByDayChart(entryModel: CartesianChartModelProducer) {
    val locale = LocalContext.current.defaultLocale

    val dateFormat = remember(locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "d MMM")

        DateTimeFormatter.ofPattern(pattern)
    }

    CartesianChartHost(
        modifier = Modifier
            .padding(
                start = 8.dp,
                top = 8.dp,
                bottom = 8.dp
            )
            .fillMaxSize(),
        scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End),
        zoomState = rememberVicoZoomState(initialZoom = remember { Zoom.min(Zoom.fixed(), Zoom.Content) }),
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
        modelProducer = entryModel
    )
}

@Composable
fun StatisticsScreen(statisticsViewModel: StatisticsViewModel = viewModel()) {
    val selectedDataType = statisticsViewModel.selectedDataType.collectAsState(initial = StatisticsViewModel.DataType.WIFIS)

    val loading = statisticsViewModel.loading.collectAsState(initial = StatisticsViewModel.State.LOADING)

    Column {
        PrimaryTabRow(
            selectedTabIndex = selectedDataType.value.ordinal
        ) {
            StatisticsViewModel.DataType.entries.map { dataType ->
                Tab(
                    selected = selectedDataType.value == dataType,
                    onClick = {
                        statisticsViewModel.setDataType(dataType)
                    },
                    text = {
                        Text(
                            text = when (dataType) {
                                StatisticsViewModel.DataType.WIFIS -> stringResource(id = R.string.wifis)
                                StatisticsViewModel.DataType.CELLS -> stringResource(id = R.string.cells)
                                StatisticsViewModel.DataType.BEACONS -> stringResource(id = R.string.beacons)
                            },
                            style = LocalTextStyle.current.copy(
                                lineBreak = LineBreak.Heading
                            )
                        )
                    }
                )
            }
        }

        ProvideVicoTheme(rememberM3VicoTheme()) {
            when (loading.value) {
                StatisticsViewModel.State.LOADING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                StatisticsViewModel.State.LOADED -> {
                    StationsByDayChart(
                        entryModel = statisticsViewModel.chartModelProducer
                    )
                }
                StatisticsViewModel.State.NO_DATA -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = stringResource(id = R.string.no_data))
                    }
                }
            }
        }
    }
}