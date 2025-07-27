package xyz.malkki.neostumbler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import java.time.LocalDate
import java.util.SortedMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.data.reports.ReportStatisticsProvider

class StatisticsViewModel(reportStatisticsProvider: ReportStatisticsProvider) : ViewModel() {
    enum class DataType {
        WIFIS,
        CELLS,
        BEACONS,
    }

    enum class State {
        LOADING,
        LOADED,
        NO_DATA,
    }

    companion object {
        val MAX_Y_VALUE_KEY = ExtraStore.Key<Long>()
    }

    private val _selectedDataType = MutableStateFlow(DataType.WIFIS)
    val selectedDataType: StateFlow<DataType>
        get() = _selectedDataType.asStateFlow()

    private val _loading = MutableStateFlow(State.LOADING)
    val loading: StateFlow<State>
        get() = _loading

    val chartModelProducer = CartesianChartModelProducer()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            _selectedDataType
                .onEach { _loading.value = State.LOADING }
                .flatMapLatest { dataType ->
                    when (dataType) {
                        DataType.WIFIS -> reportStatisticsProvider.getNewWifisPerDay()
                        DataType.CELLS -> reportStatisticsProvider.getNewCellsPerDay()
                        DataType.BEACONS -> reportStatisticsProvider.getNewBluetoothsPerDay()
                    }
                }
                .map { cumulativeSum(it.toSortedMap()).map { it.key.toEpochDay() to it.value } }
                .onEach {
                    _loading.value =
                        if (it.isEmpty()) {
                            State.NO_DATA
                        } else {
                            State.LOADED
                        }
                }
                .collectLatest { chartData ->
                    chartModelProducer.runTransaction {
                        if (chartData.isNotEmpty()) {
                            val x = chartData.map { it.first }
                            val y = chartData.map { it.second }

                            lineSeries {
                                series(x = x, y = y)

                                extras { it[MAX_Y_VALUE_KEY] = y.max() }
                            }
                        }
                    }
                }
        }
    }

    fun setDataType(dataType: DataType) {
        _selectedDataType.value = dataType
    }

    private fun cumulativeSum(data: SortedMap<LocalDate, Long>): Map<LocalDate, Long> {
        if (data.isEmpty()) {
            return emptyMap()
        }

        val out = mutableMapOf<LocalDate, Long>()
        val first: LocalDate = data.firstKey().minusDays(1)
        // Add 0 for the first day that we don't have data for so that the chart begins from zero
        out[first] = 0

        var cumul = 0L
        var prev = first
        data.forEach { (key, value) ->
            // Add data for missing days to avoid interpolating on the chart
            var d = prev.plusDays(1)
            while (d < key) {
                out[d] = cumul
                d = d.plusDays(1)
            }

            cumul += value
            out[key] = cumul

            prev = key
        }

        return out
    }
}
