package xyz.malkki.neostumbler.ui.viewmodel

import android.Manifest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import org.geohex.geohex4j.GeoHex
import org.osmdroid.util.GeoPoint
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
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
                .map { reportWithLocation ->
                    GeoHex.encode(reportWithLocation.latitude, reportWithLocation.longitude, 9)
                }
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