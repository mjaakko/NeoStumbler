package xyz.malkki.neostumbler.ui.composables.reports.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import okhttp3.Call
import org.koin.compose.koinInject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Projection
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import timber.log.Timber
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.core.report.Report
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.domain.asMapLibreLatLng
import xyz.malkki.neostumbler.geography.LatLng
import xyz.malkki.neostumbler.ichnaea.Geolocate
import xyz.malkki.neostumbler.ichnaea.IchnaeaClient
import xyz.malkki.neostumbler.ichnaea.dto.BluetoothBeaconDto
import xyz.malkki.neostumbler.ichnaea.dto.GeolocateRequestDto
import xyz.malkki.neostumbler.ichnaea.dto.GeolocateResponseDto
import xyz.malkki.neostumbler.ichnaea.dto.WifiAccessPointDto
import xyz.malkki.neostumbler.ichnaea.dto.latLng
import xyz.malkki.neostumbler.ichnaea.mapper.getIchnaeaParams
import xyz.malkki.neostumbler.ui.composables.shared.ComposableMap
import xyz.malkki.neostumbler.utils.maplibre.needsRecreation

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

private const val MAP_ZOOM_LEVEL = 15.0

private const val LINE_SOURCE_ID = "report-details-line"

private const val CIRCLE_SOURCE_ID = "report-details-locations"

@Composable
fun ReportMap(report: Report, modifier: Modifier = Modifier) {
    val estimatedLocation by getEstimatedReportLocation(report)

    var lineGeoJsonSource by remember { mutableStateOf(GeoJsonSource(LINE_SOURCE_ID)) }

    var circleGeoJsonSource by remember { mutableStateOf(GeoJsonSource(CIRCLE_SOURCE_ID)) }

    val circleFeatures =
        remember(estimatedLocation) {
            val reportLocationFeature =
                Feature.fromGeometry(
                    Point.fromLngLat(
                        report.position.position.longitude,
                        report.position.position.latitude,
                    ),
                    JsonObject().apply {
                        addProperty("accuracy", report.position.position.accuracy ?: 0.0)
                    },
                    "report-location",
                )

            val estimatedLocationFeature =
                estimatedLocation?.let {
                    Feature.fromGeometry(
                        Point.fromLngLat(it.location.lng, it.location.lat),
                        JsonObject().apply { addProperty("accuracy", it.accuracy) },
                        "estimated-location",
                    )
                }

            listOfNotNull(reportLocationFeature, estimatedLocationFeature)
        }

    val lineFeature =
        remember(estimatedLocation) {
            estimatedLocation?.location?.let {
                LineString.fromLngLats(
                    listOf(
                        Point.fromLngLat(
                            report.position.position.longitude,
                            report.position.position.latitude,
                        ),
                        Point.fromLngLat(it.lng, it.lat),
                    )
                )
            }
        }

    LaunchedEffect(lineFeature) { lineFeature?.let { lineGeoJsonSource.setGeoJson(it) } }

    Box(modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.TopCenter) {
        ComposableMap(
            onInit = { map, _ ->
                val cameraPos = report.position.position.latLng.asMapLibreLatLng()

                map.cameraPosition =
                    CameraPosition.Builder().target(cameraPos).zoom(MAP_ZOOM_LEVEL).build()

                map.uiSettings.setAllGesturesEnabled(false)
            },
            onStyleUpdated = { style ->
                if (circleGeoJsonSource.needsRecreation()) {
                    circleGeoJsonSource = GeoJsonSource(CIRCLE_SOURCE_ID)
                }
                if (lineGeoJsonSource.needsRecreation()) {
                    lineGeoJsonSource = GeoJsonSource(LINE_SOURCE_ID)
                }

                style.addSource(circleGeoJsonSource)

                style.addSource(lineGeoJsonSource)

                style.addLayer(createDistanceLineLayer(lineGeoJsonSource.id))

                style.addLayer(createLocationAccuracyLayer(circleGeoJsonSource.id))

                style.addLayer(createLocationLayer(circleGeoJsonSource.id))
            },
            updateMap = { map ->
                estimatedLocation?.let {
                    val actualLocationLatLng = report.position.position.latLng

                    map.setCameraPositionToContain(
                        listOf(
                            actualLocationLatLng to (report.position.position.accuracy ?: 0.0),
                            it.location.latLng to it.accuracy,
                        )
                    )
                }

                // Add meters per pixel property after moving the map to render circles in real
                // world size
                circleFeatures.addMeterPerPixelProperty(map.projection)

                circleGeoJsonSource.setGeoJson(FeatureCollection.fromFeatures(circleFeatures))
            },
        )

        if (estimatedLocation != null) {
            EstimatedDistance(
                reportLocation = report.position.position.latLng,
                estimatedLocation = estimatedLocation!!.location.latLng,
            )
        }
    }
}

private const val LINE_WIDTH = 2f
private const val LINE_OPACITY = 0.5f

private fun createDistanceLineLayer(sourceId: String): LineLayer {
    return LineLayer("location-distance", sourceId).apply {
        setProperties(
            PropertyFactory.lineColor(Expression.color(Color.Black.toArgb())),
            PropertyFactory.lineWidth(LINE_WIDTH),
            PropertyFactory.lineOpacity(LINE_OPACITY),
        )
    }
}

private const val ACCURACY_CIRCLE_OPACITY = 0.2f

private const val LOCATION_CIRCLE_RADIUS = 4f
private const val LOCATION_CIRCLE_BORDER = 1.5f

private fun createLocationAccuracyLayer(sourceId: String): CircleLayer {
    return CircleLayer("location-accuracy", sourceId).apply {
        setProperties(
            PropertyFactory.circleRadius(
                Expression.division(Expression.get("accuracy"), Expression.get("metersPerPixel"))
            ),
            PropertyFactory.circleColor(
                Expression.switchCase(
                    Expression.eq(Expression.id(), "estimated-location"),
                    Expression.color(Color.Magenta.toArgb()),
                    Expression.color(Color.Blue.toArgb()),
                )
            ),
            PropertyFactory.circleOpacity(ACCURACY_CIRCLE_OPACITY),
            PropertyFactory.circleStrokeWidth(1f),
            PropertyFactory.circleStrokeOpacity(2 * ACCURACY_CIRCLE_OPACITY),
            PropertyFactory.circleStrokeColor(
                Expression.switchCase(
                    Expression.eq(Expression.id(), "estimated-location"),
                    Expression.color(Color.Magenta.toArgb()),
                    Expression.color(Color.Blue.toArgb()),
                )
            ),
        )
    }
}

private fun createLocationLayer(sourceId: String): CircleLayer {
    return CircleLayer("location", sourceId).apply {
        setProperties(
            PropertyFactory.circleRadius(LOCATION_CIRCLE_RADIUS),
            PropertyFactory.circleColor(
                Expression.switchCase(
                    Expression.eq(Expression.id(), "estimated-location"),
                    Expression.color(Color.Magenta.toArgb()),
                    Expression.color(Color.Blue.toArgb()),
                )
            ),
            PropertyFactory.circleOpacity(1f),
            PropertyFactory.circleStrokeWidth(LOCATION_CIRCLE_BORDER),
            PropertyFactory.circleStrokeOpacity(1f),
            PropertyFactory.circleStrokeColor(Expression.color(Color.White.toArgb())),
        )
    }
}

private fun List<Feature>.addMeterPerPixelProperty(projection: Projection) {
    forEach { feature ->
        val point = feature.geometry() as Point

        feature.addNumberProperty(
            "metersPerPixel",
            projection.getMetersPerPixelAtLatitude(point.latitude()),
        )
    }
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

private val GEOLOCATE_RETRY_DELAY = 20.seconds

@Composable
private fun getEstimatedReportLocation(
    report: Report,
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

    return produceState(null, report, geolocate.value) {
        val flow = flow {
            val locateResponse =
                geolocate.value?.getLocation(
                    GeolocateRequestDto(
                        considerIp = false,
                        bluetoothBeacons =
                            report.bluetoothBeacons.map {
                                BluetoothBeaconDto(
                                    macAddress = it.emitter.macAddress.value,
                                    signalStrength = it.emitter.signalStrength,
                                )
                            },
                        wifiAccessPoints =
                            report.wifiAccessPoints.map {
                                WifiAccessPointDto(
                                    macAddress = it.emitter.macAddress.value,
                                    signalStrength = it.emitter.signalStrength,
                                )
                            },
                        cellTowers =
                            report.cellTowers
                                .filter { it.emitter.cellId != null }
                                .map {
                                    GeolocateRequestDto.CellTowerDto(
                                        radioType = it.emitter.radioType.name.lowercase(),
                                        mobileCountryCode =
                                            it.emitter.mobileCountryCode!!.toIntOrNull()!!,
                                        mobileNetworkCode =
                                            it.emitter.mobileNetworkCode!!.toIntOrNull()!!,
                                        locationAreaCode = it.emitter.locationAreaCode,
                                        cellId = it.emitter.cellId,
                                        signalStrength = it.emitter.signalStrength,
                                        psc = it.emitter.primaryScramblingCode,
                                        timingAdvance = it.emitter.timingAdvance,
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
