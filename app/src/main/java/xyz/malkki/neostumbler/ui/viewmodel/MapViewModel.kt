package xyz.malkki.neostumbler.ui.viewmodel

import android.Manifest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.geohex.geohex4j.GeoHex
import org.osmdroid.util.GeoPoint
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.parallelMap
import xyz.malkki.neostumbler.location.LocationSourceProvider
import kotlin.time.Duration.Companion.seconds

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val locationSource = LocationSourceProvider(getApplication()).getLocationSource()

    private val db = getApplication<StumblerApplication>().reportDb

    val latestReportPosition = liveData {
        emit(db.positionDao().getLatestPosition())
    }

    val heatMapTiles = db.reportDao().getAllReportsWithLocation()
        .distinctUntilChanged()
        .map { reportsWithLocation ->
            reportsWithLocation
                .asFlow()
                .parallelMap { reportWithLocation ->
                    GeoHex.encode(reportWithLocation.latitude, reportWithLocation.longitude, 9)
                }
                .toList()
                .groupingBy { it }
                .eachCount()
                .map {
                    HeatMapTile(
                        GeoHex.getZoneByCode(it.key).hexCoords.map { coord ->
                            GeoPoint(coord.lat, coord.lon)
                        },
                        it.value
                    )
                }
        }
        .flowOn(Dispatchers.Default)
        .asLiveData()

    private val showMyLocation = MutableLiveData(getApplication<StumblerApplication>().checkMissingPermissions(Manifest.permission.ACCESS_COARSE_LOCATION).isEmpty())

    val myLocation = showMyLocation
        .asFlow()
        .distinctUntilChanged()
        .flatMapLatest {
            if (it) {
                locationSource.getLocations(2.seconds)
            } else {
                emptyFlow()
            }
        }

    fun setShowMyLocation(value: Boolean) {
        showMyLocation.postValue(value)
    }

    data class HeatMapTile(val outline: List<GeoPoint>, val heat: Int)
}