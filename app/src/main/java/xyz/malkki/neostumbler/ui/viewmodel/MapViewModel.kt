package xyz.malkki.neostumbler.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import androidx.collection.MutableLongIntMap
import androidx.collection.MutableObjectIntMap
import androidx.collection.MutableObjectList
import androidx.collection.ObjectList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonObject
import java.io.IOException
import kotlin.math.abs
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import okhttp3.Call
import org.geohex.geohex4j.GeoHex
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.core.report.ReportWithLocation
import xyz.malkki.neostumbler.data.location.LocationSource
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.geography.LatLng
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
    settings: Settings,
    private val httpClientProvider: Deferred<Call.Factory>,
    private val reportProvider: ReportProvider,
    private val locationSource: LocationSource,
) : AndroidViewModel(application) {
    val coverageTileJsonUrl: Flow<String?> =
        settings.getSnapshotFlow().map { prefs ->
            val coverageLayerEnabled = prefs.getBoolean(PreferenceKeys.COVERAGE_LAYER_ENABLED)

            if (coverageLayerEnabled != false) {
                prefs.getString(PreferenceKeys.COVERAGE_TILE_JSON_URL)
            } else {
                null
            }
        }

    val coverageTileJsonLayerIds: Flow<List<String>> =
        coverageTileJsonUrl
            .mapLatest { coverageTileJsonUrl ->
                coverageTileJsonUrl?.let { getTileJsonLayerIds(it, httpClientProvider.await()) }
                    ?: emptyList()
            }
            .retryWhen { cause, _ ->
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

    private val _mapViewport = MutableStateFlow(LatLng.ORIGIN to DEFAULT_MAP_ZOOM)
    val mapViewport: Flow<Pair<LatLng, Double>>
        get() = _mapViewport.asStateFlow()

    private val mapBounds = Channel<Pair<LatLng, LatLng>>(capacity = Channel.CONFLATED)

    val heatMapPolygons =
        mapBounds
            .receiveAsFlow()
            .debounce(0.2.seconds)
            .flatMapLatest { bounds ->
                val (minLat, minLon) = bounds.first
                val (maxLat, maxLon) = bounds.second

                reportProvider.getReportsInsideBoundingBox(
                    minLatitude = minLat,
                    minLongitude = minLon,
                    maxLatitude = maxLat,
                    maxLongitude = maxLon,
                )
            }
            .combine(
                flow =
                    mapViewport
                        .map { (_, zoom) -> mapZoomToGeohexResolution(zoom) }
                        .distinctUntilChanged(),
                transform = { a, b -> a to b },
            )
            .mapLatest { (reportsWithLocation, resolution) ->
                val hexagons = reportsWithLocation.calculateHeatMapTiles(resolution)

                Array<Feature>(hexagons.size) { index ->
                    val heatMapTile = hexagons[index]

                    val geometry =
                        Polygon.fromLngLats(
                            listOf(
                                heatMapTile.outline.map { (lat, lng) -> Point.fromLngLat(lng, lat) }
                            )
                        )

                    Feature.fromGeometry(
                        geometry,
                        JsonObject().apply { addProperty("pct", heatMapTile.heatPct) },
                        heatMapTile.id,
                    )
                }
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
            val latestReportLocation =
                reportProvider
                    .getLatestReportLocation()
                    .map { report -> report?.let { LatLng(it.latitude, it.longitude) } }
                    .first()

            if (_mapViewport.value.first.isOrigin() && latestReportLocation != null) {
                _mapViewport.value = latestReportLocation to DEFAULT_MAP_ZOOM
            }
        }
    }

    fun setShowMyLocation(value: Boolean) {
        showMyLocation.value = value
    }

    fun setMapViewport(center: LatLng, zoom: Double) {
        if (center != LatLng.ORIGIN || zoom != DEFAULT_MAP_ZOOM) {
            this._mapViewport.value = center to zoom
        }
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
}

/** @property heatPct From 0.0 to 1.0 */
private data class HeatMapTile(val id: String, val outline: List<LatLng>, val heatPct: Float)

private const val COORDINATE_PRECISION = 0.0001f

@Suppress("MagicNumber")
private suspend fun List<ReportWithLocation>.calculateHeatMapTiles(
    resolution: Int
): ObjectList<HeatMapTile> {
    val truncatedCoords = MutableLongIntMap(size)

    forEach {
        currentCoroutineContext().ensureActive()

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
        currentCoroutineContext().ensureActive()

        val lat = Float.fromBits((packedCoord shr 32).toInt()).toDouble()
        val lng = Float.fromBits((packedCoord and 0xFFFFFFFF).toInt()).toDouble()

        val hexKey = GeoHex.encode(lat, lng, resolution)!!

        countByHexagon.put(hexKey, countByHexagon.getOrDefault(hexKey, 0) + count)
    }

    val heatMapTiles = MutableObjectList<HeatMapTile>(countByHexagon.size)

    countByHexagon.forEach { hexKey, count ->
        currentCoroutineContext().ensureActive()

        val zone = GeoHex.getZoneByCode(hexKey)

        heatMapTiles.add(
            HeatMapTile(
                id = hexKey,
                outline = zone.hexCoords.map { coord -> LatLng(coord.lat, coord.lon) },
                heatPct = ((count * REPORT_SIZE) / zone.hexSize).coerceAtMost(1.0).toFloat(),
            )
        )
    }

    return heatMapTiles
}
