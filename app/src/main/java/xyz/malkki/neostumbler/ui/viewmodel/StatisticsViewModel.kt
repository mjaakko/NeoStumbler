package xyz.malkki.neostumbler.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.StumblerApplication
import java.util.SortedMap

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val statisticsDao = (application as StumblerApplication).reportDb.statisticsDao()

    val wifiEntryModel = ChartEntryModelProducer(emptyList<ChartEntry>())

    val cellEntryModel = ChartEntryModelProducer(emptyList<ChartEntry>())

    val beaconEntryModel = ChartEntryModelProducer(emptyList<ChartEntry>())

    init {
        viewModelScope.launch {
            statisticsDao.newWifisPerDay()
                .distinctUntilChanged()
                .map { cumulativeSum(it.toSortedMap()) }
                .map {
                    it.map { (date, count) ->
                        entryOf(date.toEpochDay(), count)
                    }
                }
                .collectLatest { entries -> wifiEntryModel.setEntries(entries) }
        }

        viewModelScope.launch {
            statisticsDao.newCellsPerDay()
                .distinctUntilChanged()
                .map { cumulativeSum(it.toSortedMap()) }
                .map {
                    it.map { (date, count) ->
                        entryOf(date.toEpochDay(), count)
                    }
                }
                .collectLatest { entries -> cellEntryModel.setEntries(entries) }
        }

        viewModelScope.launch {
            statisticsDao.newBeaconsPerDay()
                .distinctUntilChanged()
                .map { cumulativeSum(it.toSortedMap()) }
                .map {
                    it.map { (date, count) ->
                        entryOf(date.toEpochDay(), count)
                    }
                }
                .collectLatest { entries -> beaconEntryModel.setEntries(entries) }
        }
    }

    private fun <K> cumulativeSum(data: SortedMap<K, Long>): Map<K, Long> {
        val out = mutableMapOf<K, Long>()

        var cumul = 0L
        data.forEach { (key, value) ->
            cumul += value

            out[key] = cumul
        }

        return out
    }
}