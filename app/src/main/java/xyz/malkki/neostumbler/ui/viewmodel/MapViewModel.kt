package xyz.malkki.neostumbler.ui.viewmodel

import android.Manifest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import org.geohex.geohex4j.GeoHex
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.common.LatLng
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.parallelMap
import xyz.malkki.neostumbler.location.LocationSourceProvider
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

//The "size" of one report relative to the geohex size. The idea is that hexes with lower resolution need more reports to show the same color
private const val REPORT_SIZE = 4

private const val GEOHEX_RESOLUTION_HIGH = 9
private const val GEOHEX_RESOLUTION_MEDIUM = 8
private const val GEOHEX_RESOLUTION_LOW = 7

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val locationSource = LocationSourceProvider(getApplication()).getLocationSource()

    private val db = getApplication<StumblerApplication>().reportDb

    private val showMyLocation = MutableLiveData(getApplication<StumblerApplication>().checkMissingPermissions(Manifest.permission.ACCESS_COARSE_LOCATION).isEmpty())

    private val _mapCenter = MutableLiveData<IGeoPoint>(GeoPoint(0.0, 0.0))
    val mapCenter: LiveData<IGeoPoint>
        get() = _mapCenter

    private val _zoom = MutableLiveData(5.0)
    val zoom: LiveData<Double>
        get() = _zoom

    private val mapBounds = Channel<Pair<LatLng, LatLng>>(capacity = Channel.Factory.CONFLATED)

    val latestReportPosition = liveData {
        emit(db.value.positionDao().getLatestPosition())
    }

    val heatMapTiles = mapBounds.receiveAsFlow()
        .debounce(0.2.seconds)
        .flatMapLatest { bounds ->
            val (minLat, minLon) = bounds.first
            val (maxLat, maxLon) = bounds.second

            val dao = db.value.reportDao()

            if (minLon > maxLon) {
                //Handle crossing the 180th meridian
                val left = dao.getAllReportsWithLocationInsideBoundingBox(
                    minLatitude = minLat,
                    minLongitude = minLon,
                    maxLatitude = maxLat,
                    maxLongitude = 180.0
                )
                val right = dao.getAllReportsWithLocationInsideBoundingBox(
                    minLatitude = -180.0,
                    minLongitude = minLon,
                    maxLatitude = maxLat,
                    maxLongitude = maxLon
                )

                left.combine(right) { listA, listB ->
                    listA + listB
                }
            } else {
                dao.getAllReportsWithLocationInsideBoundingBox(
                    minLatitude = minLat,
                    minLongitude = minLon,
                    maxLatitude = maxLat,
                    maxLongitude = maxLon
                )
            }
        }
        .distinctUntilChanged()
        .combine(
            flow = zoom.asFlow()
                .map { zoom ->
                    if (zoom >= 13.5) {
                        GEOHEX_RESOLUTION_HIGH
                    } else if (zoom >= 11.5) {
                        GEOHEX_RESOLUTION_MEDIUM
                    } else {
                        GEOHEX_RESOLUTION_LOW
                    }
                }
                .distinctUntilChanged(),
            transform = { a, b -> a to b }
        )
        .map { (reportsWithLocation, resolution) ->
            reportsWithLocation
                .asFlow()
                .parallelMap { reportWithLocation ->
                    GeoHex.encode(reportWithLocation.latitude, reportWithLocation.longitude, resolution)
                }
                .toList()
                .groupingBy { it }
                .eachCount()
                .map {
                    val zone = GeoHex.getZoneByCode(it.key)

                    HeatMapTile(
                        zone.hexCoords.map { coord ->
                            GeoPoint(coord.lat, coord.lon)
                        },
                        ((it.value * REPORT_SIZE) / zone.hexSize).coerceAtMost(1.0).toFloat()
                    )
                }
        }
        .flowOn(Dispatchers.Default)
        .asLiveData()

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

    fun setMapCenter(mapCenter: IGeoPoint) = this._mapCenter.postValue(mapCenter)

    fun setZoom(zoom: Double) = this._zoom.postValue(zoom)

    fun setMapBounds(minLatitude: Double, minLongitude: Double, maxLatitude: Double, maxLongitude: Double) {
        //Make the bounds slightly larger so that data in the map edges will be visible
        val latAdjust = abs(maxLatitude - minLatitude) * 0.1
        val lngAdjust = if (minLongitude > maxLongitude) {
            (360.0 + maxLongitude) - minLongitude
        } else {
            maxLongitude - minLongitude
        } * 0.1

        val bounds = LatLng(minLatitude - latAdjust, minLongitude - lngAdjust) to LatLng(maxLatitude + latAdjust, maxLongitude + lngAdjust)

        mapBounds.trySendBlocking(bounds)
    }

    /**
     * @property heatPct From 0.0 to 1.0
     */
    data class HeatMapTile(val outline: List<GeoPoint>, val heatPct: Float)
}