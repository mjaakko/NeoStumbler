package xyz.malkki.neostumbler.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.db.dao.StatisticsDao
import java.time.LocalDate
import java.util.SortedMap

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val MAX_Y_VALUE_KEY = ExtraStore.Key<Long>()
    }

    private val statisticsDao: Flow<StatisticsDao> = (application as StumblerApplication).reportDb.mapLatest { it.statisticsDao() }

    val wifiEntryModel = CartesianChartModelProducer()

    val cellEntryModel = CartesianChartModelProducer()

    val beaconEntryModel = CartesianChartModelProducer()

    private val wifisLoaded = MutableLiveData(false)
    private val cellsLoaded = MutableLiveData(false)
    private val beaconsLoaded = MutableLiveData(false)

    val dataLoaded = MediatorLiveData<Boolean>()
        .apply {
            val setDataLoaded = { _: Boolean ->
                value = wifisLoaded.value == true && cellsLoaded.value == true && beaconsLoaded.value == true
            }

            addSource(wifisLoaded, setDataLoaded)
            addSource(cellsLoaded, setDataLoaded)
            addSource(beaconsLoaded, setDataLoaded)
        }
        .distinctUntilChanged()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            statisticsDao
                .flatMapLatest {
                    it.newWifisPerDay()
                }
                .distinctUntilChanged()
                .map { cumulativeSum(it.toSortedMap()) }
                .onEach { wifisLoaded.postValue(true) }
                .collectLatest { entries ->
                    val chartData = entries
                        .map {
                            it.key.toEpochDay() to it.value
                        }

                    if (chartData.isNotEmpty()) {
                        wifiEntryModel.runTransaction {
                            val x = chartData.map { it.first }
                            val y = chartData.map { it.second }

                            lineSeries {
                                series(x = x, y = y)

                                extras {
                                    it[MAX_Y_VALUE_KEY] = y.max()
                                }
                            }
                        }
                    }
                }
        }

        viewModelScope.launch(Dispatchers.Default)  {
            statisticsDao
                .flatMapLatest {
                    it.newCellsPerDay()
                }
                .distinctUntilChanged()
                .map { cumulativeSum(it.toSortedMap()) }
                .onEach { cellsLoaded.postValue(true) }
                .collectLatest { entries ->
                    val chartData = entries
                        .map {
                            it.key.toEpochDay() to it.value
                        }

                    if (chartData.isNotEmpty()) {
                        cellEntryModel.runTransaction {
                            val x = chartData.map { it.first }
                            val y = chartData.map { it.second }

                            lineSeries {
                                series(x = x, y = y)

                                extras {
                                    it[MAX_Y_VALUE_KEY] = y.max()
                                }
                            }
                        }
                    }
                }
        }

        viewModelScope.launch(Dispatchers.Default)  {
            statisticsDao
                .flatMapLatest {
                    it.newBeaconsPerDay()
                }
                .distinctUntilChanged()
                .map { cumulativeSum(it.toSortedMap()) }
                .onEach { beaconsLoaded.postValue(true) }
                .collectLatest { entries ->
                    val chartData = entries
                        .map {
                            it.key.toEpochDay() to it.value
                        }

                    if (chartData.isNotEmpty()) {
                        beaconEntryModel.runTransaction {
                            val x = chartData.map { it.first }
                            val y = chartData.map { it.second }

                            lineSeries {
                                series(x = x, y = y)

                                extras {
                                    it[MAX_Y_VALUE_KEY] = y.max()
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun cumulativeSum(data: SortedMap<LocalDate, Long>): Map<LocalDate, Long> {
        if (data.isEmpty()) {
            return emptyMap()
        }

        val out = mutableMapOf<LocalDate, Long>()
        //Add 0 for the first day that we don't have data for so that the chart begins from zero
        out[data.firstKey().minusDays(1)] = 0

        var cumul = 0L
        data.forEach { (key, value) ->
            cumul += value

            out[key] = cumul
        }

        return out
    }
}