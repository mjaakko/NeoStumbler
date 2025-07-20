package xyz.malkki.neostumbler.ui.composables.reports.details

import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import okhttp3.Call
import org.koin.compose.koinInject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Projection
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.plugins.annotation.CircleManager
import org.maplibre.android.plugins.annotation.CircleOptions
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.utils.ColorUtils
import timber.log.Timber
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getEnum
import xyz.malkki.neostumbler.db.entities.ReportWithData
import xyz.malkki.neostumbler.db.entities.latLng
import xyz.malkki.neostumbler.domain.asMapLibreLatLng
import xyz.malkki.neostumbler.geography.LatLng
import xyz.malkki.neostumbler.ichnaea.Geolocate
import xyz.malkki.neostumbler.ichnaea.IchnaeaClient
import xyz.malkki.neostumbler.ichnaea.dto.BluetoothBeaconDto
import xyz.malkki.neostumbler.ichnaea.dto.CellTowerDto
import xyz.malkki.neostumbler.ichnaea.dto.GeolocateRequestDto
import xyz.malkki.neostumbler.ichnaea.dto.GeolocateResponseDto
import xyz.malkki.neostumbler.ichnaea.dto.WifiAccessPointDto
import xyz.malkki.neostumbler.ichnaea.dto.latLng
import xyz.malkki.neostumbler.ichnaea.mapper.getIchnaeaParams
import xyz.malkki.neostumbler.ui.composables.shared.CenteredCircularProgressIndicator
import xyz.malkki.neostumbler.ui.map.LifecycleAwareMapView
import xyz.malkki.neostumbler.ui.map.MapTileSource
import xyz.malkki.neostumbler.ui.map.setAttributionMargin
import xyz.malkki.neostumbler.ui.map.updateMapStyleIfNeeded

private const val MAP_ZOOM_LEVEL = 15.0

private fun Settings.mapStyleUrl(): Flow<String> =
    getSnapshotFlow().map { prefs ->
        val tileSource = prefs.getEnum(PreferenceKeys.MAP_TILE_SOURCE) ?: MapTileSource.DEFAULT

        if (tileSource == MapTileSource.CUSTOM) {
            prefs.getString(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_URL) ?: ""
        } else {
            tileSource.sourceUrl!!
        }
    }

@Composable
fun ReportMap(
    reportWithData: ReportWithData,
    modifier: Modifier = Modifier,
    settings: Settings = koinInject(),
    httpClientProvider: Deferred<Call.Factory> = koinInject(),
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val density = LocalDensity.current

    val mapStyleUrl by settings.mapStyleUrl().collectAsState(initial = null)

    val httpClient = produceState<Call.Factory?>(null) { value = httpClientProvider.await() }

    val estimatedLocation = getEstimatedReportLocation(reportWithData)

    if (mapStyleUrl == null || httpClient.value == null) {
        CenteredCircularProgressIndicator(modifier = modifier.fillMaxWidth().height(150.dp))
    } else {
        val circleManager = remember { mutableStateOf<CircleManager?>(null) }

        val lineManager = remember { mutableStateOf<LineManager?>(null) }

        Box(modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.TopCenter) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapLibre.getInstance(context)
                    HttpRequestUtil.setOkHttpClient(httpClient.value)

                    val mapView = LifecycleAwareMapView(context)
                    mapView.localizeLabelNames()

                    mapView.getMapAsync { map ->
                        val cameraPos = reportWithData.positionEntity.latLng.asMapLibreLatLng()

                        map.cameraPosition =
                            CameraPosition.Builder().target(cameraPos).zoom(MAP_ZOOM_LEVEL).build()

                        map.setAttributionMargin(density)

                        map.uiSettings.setAllGesturesEnabled(false)

                        val styleBuilder = Style.Builder().fromUri(mapStyleUrl!!)

                        map.setStyle(styleBuilder) { style ->
                            lineManager.value = LineManager(mapView, map, style)

                            circleManager.value = CircleManager(mapView, map, style)
                        }
                    }

                    mapView
                },
                update = { mapView ->
                    mapView.lifecycle = lifecycle

                    val lineManager = lineManager.value

                    val circleManager = circleManager.value

                    mapView.getMapAsync { map ->
                        mapStyleUrl?.let { map.updateMapStyleIfNeeded(it) }

                        if (circleManager != null && lineManager != null) {
                            circleManager.deleteAll()

                            if (estimatedLocation.value != null) {
                                val actualLocationLatLng = reportWithData.positionEntity.latLng
                                val estimatedLocationLatLng =
                                    estimatedLocation.value!!.location.latLng

                                map.setCameraPositionToContain(
                                    listOf(
                                        actualLocationLatLng to
                                            (reportWithData.positionEntity.accuracy ?: 0.0),
                                        estimatedLocationLatLng to
                                            estimatedLocation.value!!.accuracy,
                                    )
                                )

                                circleManager.drawLocationCircle(
                                    projection = map.projection,
                                    center = estimatedLocation.value!!.location.latLng,
                                    radius = estimatedLocation.value!!.accuracy,
                                    color = Color.Magenta.toArgb(),
                                )

                                lineManager.drawLineBetweenActualAndEstimatedLocation(
                                    actual = actualLocationLatLng,
                                    estimated = estimatedLocationLatLng,
                                )
                            }

                            circleManager.drawLocationCircle(
                                projection = map.projection,
                                center =
                                    LatLng(
                                        reportWithData.positionEntity.latitude,
                                        reportWithData.positionEntity.longitude,
                                    ),
                                radius = reportWithData.positionEntity.accuracy ?: 0.0,
                                color = Color.Blue.toArgb(),
                            )
                        }
                    }
                },
                onRelease = { view -> view.lifecycle = null },
            )

            if (estimatedLocation.value != null) {
                EstimatedDistance(
                    reportLocation = reportWithData.positionEntity.latLng,
                    estimatedLocation = estimatedLocation.value!!.location.latLng,
                )
            }
        }
    }
}

@Composable
private fun EstimatedDistance(reportLocation: LatLng, estimatedLocation: LatLng) {
    val distance = reportLocation.distanceTo(estimatedLocation).roundToInt()

    Text(
        text = stringResource(R.string.distance_to_estimated_location, distance),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.MiddleEllipsis,
    )
}

// Map padding when showing both the actual and the estimated location
private const val MAP_PADDING = 100

private fun MapLibreMap.setCameraPositionToContain(circles: List<Pair<LatLng, Double>>) {
    val bounds = circles.flatMap { getBoundingBoxLatLngs(it.first, it.second) }

    // Fit both locations on the map
    val cameraPosition =
        CameraUpdateFactory.newLatLngBounds(
                bounds = LatLngBounds.fromLatLngs(bounds.map { it.asMapLibreLatLng() }),
                padding = MAP_PADDING,
            )
            .getCameraPosition(this)!!

    this.cameraPosition =
        if (cameraPosition.zoom > MAP_ZOOM_LEVEL) {
            CameraPosition.Builder(cameraPosition).zoom(MAP_ZOOM_LEVEL).build()
        } else {
            cameraPosition
        }
}

private const val DIRECTION_WEST = 270.0
private const val DIRECTION_SOUTH = 180.0
private const val DIRECTION_NORTH = 0.0
private const val DIRECTION_EAST = 90.0

private fun getBoundingBoxLatLngs(center: LatLng, radius: Double): List<LatLng> {
    return listOf(
        center.destination(radius, DIRECTION_WEST).destination(radius, DIRECTION_SOUTH),
        center.destination(radius, DIRECTION_NORTH).destination(radius, DIRECTION_EAST),
    )
}

private const val LINE_WIDTH = 2f
private const val LINE_OPACITY = 0.5f

private fun LineManager.drawLineBetweenActualAndEstimatedLocation(
    actual: LatLng,
    estimated: LatLng,
) {
    deleteAll()

    create(
        LineOptions()
            .withLatLngs(listOf(actual.asMapLibreLatLng(), estimated.asMapLibreLatLng()))
            .withLineColor(ColorUtils.colorToRgbaString(Color.Black.toArgb()))
            .withLineWidth(LINE_WIDTH)
            .withLineOpacity(LINE_OPACITY)
    )
}

private const val ACCURACY_CIRCLE_OPACITY = 0.2f

private const val LOCATION_CIRCLE_RADIUS = 4f
private const val LOCATION_CIRCLE_BORDER = 1.5f

private fun CircleManager.drawLocationCircle(
    projection: Projection,
    center: LatLng,
    radius: Double,
    @ColorInt color: Int,
) {
    val radiusDp = radius / projection.getMetersPerPixelAtLatitude(center.latitude)

    create(
        CircleOptions()
            .withLatLng(center.asMapLibreLatLng())
            .withCircleRadius(radiusDp.toFloat())
            .withCircleColor(ColorUtils.colorToRgbaString(color))
            .withCircleOpacity(ACCURACY_CIRCLE_OPACITY)
            .withCircleStrokeWidth(1f)
            .withCircleStrokeOpacity(2 * ACCURACY_CIRCLE_OPACITY)
            .withCircleStrokeColor(ColorUtils.colorToRgbaString(color))
    )

    create(
        CircleOptions()
            .withLatLng(center.asMapLibreLatLng())
            .withCircleRadius(LOCATION_CIRCLE_RADIUS)
            .withCircleColor(ColorUtils.colorToRgbaString(color))
            .withCircleOpacity(1f)
            .withCircleStrokeWidth(LOCATION_CIRCLE_BORDER)
            .withCircleStrokeOpacity(1f)
            .withCircleStrokeColor(ColorUtils.colorToRgbaString(Color.White.toArgb()))
    )
}

private val GEOLOCATE_RETRY_DELAY = 20.seconds

@Composable
private fun getEstimatedReportLocation(
    reportWithData: ReportWithData,
    settings: Settings = koinInject(),
    httpClientProvider: Deferred<Call.Factory> = koinInject(),
): State<GeolocateResponseDto?> {
    val geolocate =
        produceState<Geolocate?>(null) {
            val ichnaeaParams = settings.getIchnaeaParams()

            if (ichnaeaParams != null) {
                value = IchnaeaClient(httpClientProvider.await(), ichnaeaParams)
            }
        }

    return produceState(null, reportWithData, geolocate.value) {
        val flow = flow {
            val locateResponse =
                geolocate.value?.getLocation(
                    GeolocateRequestDto(
                        considerIp = false,
                        bluetoothBeacons =
                            reportWithData.bluetoothBeaconEntities.map {
                                BluetoothBeaconDto(
                                    macAddress = it.macAddress,
                                    signalStrength = it.signalStrength,
                                )
                            },
                        wifiAccessPoints =
                            reportWithData.wifiAccessPointEntities.map {
                                WifiAccessPointDto(
                                    macAddress = it.macAddress,
                                    signalStrength = it.signalStrength,
                                )
                            },
                        cellTowers =
                            reportWithData.cellTowerEntities
                                .filter { it.cellId != null }
                                .map {
                                    CellTowerDto(
                                        radioType = it.radioType,
                                        mobileCountryCode = it.mobileCountryCode!!.toIntOrNull()!!,
                                        mobileNetworkCode = it.mobileNetworkCode!!.toIntOrNull()!!,
                                        locationAreaCode = it.locationAreaCode,
                                        cellId = it.cellId,
                                        signalStrength = it.signalStrength,
                                    )
                                },
                    )
                )

            emit(locateResponse)
        }

        flow
            .retryWhen { ex, _ ->
                if (ex is IOException) {
                    Timber.w(ex, "Failed to find a location for the report")

                    delay(GEOLOCATE_RETRY_DELAY)
                } else {
                    throw ex
                }

                true
            }
            .collect { value = it }
    }
}
