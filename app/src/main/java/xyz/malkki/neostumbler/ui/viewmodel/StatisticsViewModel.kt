package xyz.malkki.neostumbler.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.StumblerApplication
import java.time.LocalDate
import java.util.SortedMap

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val statisticsDao = (application as StumblerApplication).reportDb.statisticsDao()

    val wifiEntryModel = ChartEntryModelProducer(emptyList<ChartEntry>())

    val cellEntryModel = ChartEntryModelProducer(emptyList<ChartEntry>())

    val beaconEntryModel = ChartEntryModelProducer(emptyList<ChartEntry>())

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
            statisticsDao.newWifisPerDay()
                .distinctUntilChanged()
                .map { cumulativeSum(it.toSortedMap()) }
                .map {
                    it.map { (date, count) ->
                        entryOf(date.toEpochDay(), count)
                    }
                }
                .onEach { wifisLoaded.postValue(true) }
                .collectLatest { entries -> wifiEntryModel.setEntries(entries) }
        }

        viewModelScope.launch(Dispatchers.Default)  {
            statisticsDao.newCellsPerDay()
                .distinctUntilChanged()
                .map { cumulativeSum(it.toSortedMap()) }
                .map {
                    it.map { (date, count) ->
                        entryOf(date.toEpochDay(), count)
                    }
                }
                .onEach { cellsLoaded.postValue(true) }
                .collectLatest { entries -> cellEntryModel.setEntries(entries) }
        }

        viewModelScope.launch(Dispatchers.Default)  {
            statisticsDao.newBeaconsPerDay()
                .distinctUntilChanged()
                .map { cumulativeSum(it.toSortedMap()) }
                .map {
                    it.map { (date, count) ->
                        entryOf(date.toEpochDay(), count)
                    }
                }
                .onEach { beaconsLoaded.postValue(true) }
                .collectLatest { entries -> beaconEntryModel.setEntries(entries) }
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