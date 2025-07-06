package xyz.malkki.neostumbler.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import androidx.collection.MutableLongIntMap
import androidx.collection.MutableObjectIntMap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlin.math.abs
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import okhttp3.Call
import org.geohex.geohex4j.GeoHex
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.db.ReportDatabaseManager
import xyz.malkki.neostumbler.db.dao.getReportsInsideBoundingBox
import xyz.malkki.neostumbler.db.entities.ReportWithLocation
import xyz.malkki.neostumbler.domain.LatLng
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.get
import xyz.malkki.neostumbler.location.LocationSource
import xyz.malkki.neostumbler.ui.map.MapTileSource
import xyz.malkki.neostumbler.ui.viewmodel.MapViewModel.HeatMapTile
import xyz.malkki.neostumbler.utils.getTileJsonLayerIds

// The "size" of one report relative to the geohex size. The idea is that hexes with lower
// resolution need more reports to show the same color
private const val REPORT_SIZE = 4

private const val DEFAULT_MAP_ZOOM = 12.0

private const val MIN_GEOHEX_RESOLUTION = 3
private const val MAX_GEOHEX_RESOLUTION = 9

private val TILEJSON_RETRY_DELAY = 30.seconds

class MapViewModel(
    application: Application,
    private val settingsStore: DataStore<Preferences>,
    private val httpClientProvider: Deferred<Call.Factory>,
    private val reportDatabaseManager: ReportDatabaseManager,
    private val locationSource: LocationSource,
) : AndroidViewModel(application) {
    private val _httpClient = MutableStateFlow<Call.Factory?>(null)
    val httpClient: StateFlow<Call.Factory?>
        get() = _httpClient.asStateFlow()

    val mapTileSourceUrl: Flow<String> =
        settingsStore.data
            .map { prefs ->
                val mapTileSource =
                    prefs.get<MapTileSource>(PreferenceKeys.MAP_TILE_SOURCE)
                        ?: MapTileSource.DEFAULT

                if (mapTileSource == MapTileSource.CUSTOM) {
                    prefs[stringPreferencesKey(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_URL)] ?: ""
                } else {
                    mapTileSource.sourceUrl!!
                }
            }
            .distinctUntilChanged()

    val coverageTileJsonUrl: Flow<String?> =
        settingsStore.data.map { prefs ->
            prefs[stringPreferencesKey(PreferenceKeys.COVERAGE_TILE_JSON_URL)]
        }

    val coverageTileJsonLayerIds: Flow<List<String>> =
        combine(coverageTileJsonUrl.filterNotNull(), httpClient.filterNotNull()) { a, b -> a to b }
            .mapLatest { (coverageTileJsonUrl, httpClient) ->
                getTileJsonLayerIds(coverageTileJsonUrl, httpClient)
            }
            .retryWhen { cause, attempt ->
                if (cause is IOException) {
                    Timber.w(cause, "Failed to load TileJSON for coverage layer")

                    delay(TILEJSON_RETRY_DELAY)

                    true
                } else {
                    throw cause
                }
            }

    private val showMyLocation =
        MutableStateFlow(
            getApplication<StumblerApplication>()
                .checkMissingPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
                .isEmpty()
        )

    private val _mapCenter = MutableStateFlow<LatLng>(LatLng.ORIGIN)
    val mapCenter: StateFlow<LatLng>
        get() = _mapCenter.asStateFlow()

    private val _zoom = MutableStateFlow(DEFAULT_MAP_ZOOM)
    val zoom: StateFlow<Double>
        get() = _zoom.asStateFlow()

    private val mapBounds = Channel<Pair<LatLng, LatLng>>(capacity = Channel.Factory.CONFLATED)

    val latestReportPosition = flow {
        emit(reportDatabaseManager.reportDb.value.positionDao().getLatestPosition())
    }

    val heatMapTiles =
        mapBounds
            .receiveAsFlow()
            .debounce(0.2.seconds)
            .flatMapLatest { bounds ->
                val (minLat, minLon) = bounds.first
                val (maxLat, maxLon) = bounds.second

                reportDatabaseManager.reportDb.value
                    .reportDao()
                    .getReportsInsideBoundingBox(
                        minLatitude = minLat,
                        minLongitude = minLon,
                        maxLatitude = maxLat,
                        maxLongitude = maxLon,
                    )
            }
            .combine(
                flow = zoom.map { zoom -> mapZoomToGeohexResolution(zoom) }.distinctUntilChanged(),
                transform = { a, b -> a to b },
            )
            .mapLatest { (reportsWithLocation, resolution) ->
                reportsWithLocation.calculateHeatMapTiles(resolution)
            }
            .flowOn(Dispatchers.Default)
            .shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    val myLocation =
        showMyLocation.flatMapLatest {
            if (it) {
                @SuppressLint("MissingPermission")
                locationSource.getLocations(2.seconds, usePassiveProvider = false)
            } else {
                emptyFlow()
            }
        }

    init {
        viewModelScope.launch {
            val httpClient = httpClientProvider.await()
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

    @Suppress("MagicNumber")
    fun setMapBounds(
        minLatitude: Double,
        minLongitude: Double,
        maxLatitude: Double,
        maxLongitude: Double,
    ) {
        // Make the bounds slightly larger so that data in the map edges will be visible
        val latAdjust = abs(maxLatitude - minLatitude) * 0.1
        val lngAdjust =
            if (minLongitude > maxLongitude) {
                (360.0 + maxLongitude) - minLongitude
            } else {
                maxLongitude - minLongitude
            } * 0.1

        val bounds =
            LatLng(minLatitude - latAdjust, minLongitude - lngAdjust) to
                LatLng(maxLatitude + latAdjust, maxLongitude + lngAdjust)

        mapBounds.trySendBlocking(bounds)
    }

    @Suppress("MagicNumber")
    private fun mapZoomToGeohexResolution(mapZoom: Double): Int {
        // Convert map zoom level to a suitable geohex resolution
        return floor(mapZoom * 0.5 + 3)
            .toInt()
            .coerceIn(MIN_GEOHEX_RESOLUTION, MAX_GEOHEX_RESOLUTION)
    }

    /** @property heatPct From 0.0 to 1.0 */
    data class HeatMapTile(val id: String, val outline: List<LatLng>, val heatPct: Float)
}

private const val COORDINATE_PRECISION = 0.0001f

@Suppress("MagicNumber")
private fun List<ReportWithLocation>.calculateHeatMapTiles(
    resolution: Int
): Map<String, HeatMapTile> {
    val truncatedCoords = MutableLongIntMap(size)

    forEach {
        // Calculating the hexagons is relatively expensive -> truncate coordinates first to avoid
        // unnecessary calculations
        val lat = it.latitude.toFloat()
        val lng = it.longitude.toFloat()

        val truncatedLat = lat - (lat % COORDINATE_PRECISION)
        val truncatedLng = lng - (lng % COORDINATE_PRECISION)

        val packedCoords =
            (truncatedLat.toRawBits().toLong() shl 32) or
                (truncatedLng.toRawBits().toLong() and 0xFFFFFFFF)

        truncatedCoords.put(packedCoords, truncatedCoords.getOrDefault(packedCoords, 0) + 1)
    }

    val countByHexagon = MutableObjectIntMap<String>()

    truncatedCoords.forEach { packedCoord, count ->
        val lat = Float.fromBits((packedCoord shr 32).toInt()).toDouble()
        val lng = Float.fromBits((packedCoord and 0xFFFFFFFF).toInt()).toDouble()

        val hexKey = GeoHex.encode(lat, lng, resolution)!!

        countByHexagon.put(hexKey, countByHexagon.getOrDefault(hexKey, 0) + count)
    }

    val heatMapTiles = HashMap<String, HeatMapTile>(countByHexagon.size)

    countByHexagon.forEach { hexKey, count ->
        val zone = GeoHex.getZoneByCode(hexKey)

        heatMapTiles[hexKey] =
            HeatMapTile(
                id = hexKey,
                outline = zone.hexCoords.map { coord -> LatLng(coord.lat, coord.lon) },
                heatPct = ((count * REPORT_SIZE) / zone.hexSize).coerceAtMost(1.0).toFloat(),
            )
    }

    return heatMapTiles
}
