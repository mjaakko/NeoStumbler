package xyz.malkki.neostumbler.ui.viewmodel

import android.Manifest
import android.app.Application
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import org.geohex.geohex4j.GeoHex
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.common.LatLng
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.get
import xyz.malkki.neostumbler.extensions.parallelMap
import xyz.malkki.neostumbler.location.LocationSourceProvider
import kotlin.math.abs
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds

//The "size" of one report relative to the geohex size. The idea is that hexes with lower resolution need more reports to show the same color
private const val REPORT_SIZE = 4

private val GEOHEX_RESOLUTION_RANGE = 3..9

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val locationSource = LocationSourceProvider(getApplication()).getLocationSource()

    private val settingsStore = getApplication<StumblerApplication>().settingsStore

    private val db = getApplication<StumblerApplication>().reportDb

    private val _httpClient = MutableStateFlow<Call.Factory?>(null)
    val httpClient: StateFlow<Call.Factory?>
        get() = _httpClient.asStateFlow()

    val mapTileSource: Flow<MapTileSource> = settingsStore.data
        .map { prefs ->
            prefs.get<MapTileSource>(PreferenceKeys.MAP_TILE_SOURCE) ?: MapTileSource.OPENSTREETMAP
        }
        .distinctUntilChanged()

    val mapStyle: Flow<MapStyle> = mapTileSource
        .map { mapTileSource ->
            if (mapTileSource.sourceAsset != null) {
                MapStyle(styleUrl = null, styleJson = readStyleFromAssets(mapTileSource.sourceAsset))
            } else {
                MapStyle(styleUrl = mapTileSource.sourceUrl!!, styleJson = null)
            }
        }
        .shareIn(viewModelScope, started = SharingStarted.Eagerly, replay = 1)

    private val showMyLocation = MutableStateFlow(getApplication<StumblerApplication>().checkMissingPermissions(Manifest.permission.ACCESS_COARSE_LOCATION).isEmpty())

    private val _mapCenter = MutableStateFlow<LatLng>(LatLng(0.0, 0.0))
    val mapCenter: StateFlow<LatLng>
        get() = _mapCenter.asStateFlow()

    private val _zoom = MutableStateFlow(5.0)
    val zoom: StateFlow<Double>
        get() = _zoom.asStateFlow()

    private val mapBounds = Channel<Pair<LatLng, LatLng>>(capacity = Channel.Factory.CONFLATED)

    val latestReportPosition = flow {
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
            flow = zoom
                .map { zoom ->
                    //Convert map zoom level to a suitable geohex resolution
                    floor(zoom * 0.5 + 3).toInt().coerceIn(GEOHEX_RESOLUTION_RANGE)
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
                            LatLng(coord.lat, coord.lon)
                        },
                        ((it.value * REPORT_SIZE) / zone.hexSize).coerceAtMost(1.0).toFloat()
                    )
                }
        }
        .flowOn(Dispatchers.Default)
        .shareIn(viewModelScope, started = SharingStarted.Lazily)

    val myLocation = showMyLocation
        .flatMapLatest {
            if (it) {
                locationSource.getLocations(2.seconds)
            } else {
                emptyFlow()
            }
        }

    init {
        viewModelScope.launch {
            val httpClient = (application as StumblerApplication).httpClientProvider.await()
            _httpClient.value = httpClient
        }
    }

    fun setShowMyLocation(value: Boolean) {
        showMyLocation.value = value
    }

    fun setMapCenter(mapCenter: LatLng) {
        this._mapCenter.value = mapCenter
    }

    fun setZoom(zoom: Double) {
        this._zoom.value = zoom
    }

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

    fun setMapTileSource(mapTileSource: MapTileSource) {
        viewModelScope.launch {
            settingsStore.updateData { prefs ->
                prefs.toMutablePreferences().apply {
                    set(stringPreferencesKey(PreferenceKeys.MAP_TILE_SOURCE), mapTileSource.name)
                }
            }
        }
    }

    private suspend fun readStyleFromAssets(assetName: String): String = withContext(Dispatchers.IO) {
        getApplication<StumblerApplication>().assets.open(assetName).use {
            it.readBytes().decodeToString()
        }
    }

    /**
     * @property heatPct From 0.0 to 1.0
     */
    data class HeatMapTile(val outline: List<LatLng>, val heatPct: Float)

    enum class MapTileSource(val title: String, val sourceUrl: String?, val sourceAsset: String?) {
        OPENSTREETMAP("OpenStreetMap", null, "osm_raster_style.json"),
        OPENFREEMAP("OpenFreeMap", "https://tiles.openfreemap.org/styles/liberty", null),
        VERSATILES("VersaTiles", "https://tiles.versatiles.org/assets/styles/colorful.json", null)
    }

    data class MapStyle(val styleUrl: String?, val styleJson: String?)
}