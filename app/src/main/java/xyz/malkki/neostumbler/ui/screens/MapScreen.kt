package xyz.malkki.neostumbler.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import androidx.annotation.ColorInt
import androidx.collection.forEach
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.JsonPrimitive
import kotlin.math.abs
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.gestures.MoveGestureDetector
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentConstants
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.Fill
import org.maplibre.android.plugins.annotation.FillManager
import org.maplibre.android.plugins.annotation.FillOptions
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.android.utils.ColorUtils as MapLibreColorUtils
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.core.observation.PositionObservation
import xyz.malkki.neostumbler.domain.asDomainLatLng
import xyz.malkki.neostumbler.domain.asMapLibreLatLng
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.ui.composables.map.MapSettingsButton
import xyz.malkki.neostumbler.ui.composables.shared.ComposableMap
import xyz.malkki.neostumbler.ui.composables.shared.KeepScreenOn
import xyz.malkki.neostumbler.ui.composables.shared.PermissionsDialog
import xyz.malkki.neostumbler.ui.viewmodel.MapViewModel

@ColorInt private const val HEAT_LOW: Int = 0x78d278ff
@ColorInt private const val HEAT_HIGH: Int = 0x78aa00ff

private const val COVERAGE_SOURCE_ID = "coverage-source"
private const val COVERAGE_LAYER_PREFIX = "coverage-layer-"

private const val COVERAGE_COLOR = "#ff8000"
private const val COVERAGE_COLOR_DARK = "#ffbb00"
private const val COVERAGE_OPACITY = 0.4f
private const val COVERAGE_OPACITY_DARK = 0.25f

private const val MIN_ZOOM = 3.0
private const val MAX_ZOOM = 15.0

// This method is long because we are mixing Compose with Views
// FIXME: try to break this into smaller pieces and then remove these suppressions
@Suppress("LongMethod")
@Composable
fun MapScreen(mapViewModel: MapViewModel = koinViewModel<MapViewModel>()) {
    val context = LocalContext.current

    val showPermissionDialog = rememberSaveable { mutableStateOf(false) }

    val trackMyLocation = rememberSaveable { mutableStateOf(false) }

    val fillManager = remember { mutableStateOf<FillManager?>(null) }

    val coverageTileJsonUrl =
        mapViewModel.coverageTileJsonUrl.collectAsStateWithLifecycle(initialValue = null)

    val coverageTileJsonLayerIds =
        mapViewModel.coverageTileJsonLayerIds.collectAsStateWithLifecycle(
            initialValue = emptyList()
        )

    val heatMapTiles =
        mapViewModel.heatMapTiles.collectAsStateWithLifecycle(initialValue = emptyMap())

    val myLocation by
        mapViewModel.myLocation.collectAsStateWithLifecycle(
            initialValue = null,
            minActiveState = Lifecycle.State.RESUMED,
        )

    val mapViewport by mapViewModel.mapViewport.collectAsStateWithLifecycle(initialValue = null)

    if (showPermissionDialog.value) {
        PermissionsDialog(
            missingPermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            permissionRationales =
                mapOf(
                    Manifest.permission.ACCESS_FINE_LOCATION to
                        stringResource(R.string.permission_rationale_location_map)
                ),
            onPermissionsGranted = {
                showPermissionDialog.value = false

                val hasPermission =
                    context
                        .checkMissingPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
                        .isEmpty()

                mapViewModel.setShowMyLocation(hasPermission)
                trackMyLocation.value = hasPermission
            },
        )
    }

    KeepScreenOn()

    Box(modifier = Modifier.fillMaxSize()) {
        ComposableMap(
            modifier = Modifier.fillMaxSize(),
            onInit = { map, mapView ->
                map.addOnMoveListener(OnMapMoveBeginListener { trackMyLocation.value = false })

                map.addOnCameraMoveListener {
                    mapViewModel.setMapViewport(
                        center = map.cameraPosition.target!!.asDomainLatLng(),
                        zoom = map.cameraPosition.zoom,
                    )

                    mapViewModel.setMapBounds(
                        minLatitude = map.projection.visibleRegion.latLngBounds.latitudeSouth,
                        maxLatitude = map.projection.visibleRegion.latLngBounds.latitudeNorth,
                        minLongitude = map.projection.visibleRegion.latLngBounds.longitudeWest,
                        maxLongitude = map.projection.visibleRegion.latLngBounds.longitudeEast,
                    )
                }

                map.setMinZoomPreference(MIN_ZOOM)
                map.setMaxZoomPreference(MAX_ZOOM)

                map.uiSettings.isRotateGesturesEnabled = false

                map.getStyle { style ->
                    map.locationComponent.activateLocationComponent(
                        LocationComponentActivationOptions.builder(context, style)
                            // Set location engine to null, because we provide locations by
                            // ourself
                            .locationEngine(null)
                            .useDefaultLocationEngine(false)
                            .useSpecializedLocationLayer(true)
                            .build()
                    )
                    @SuppressLint("MissingPermission")
                    map.locationComponent.isLocationComponentEnabled = true
                    map.locationComponent.renderMode = RenderMode.COMPASS

                    fillManager.value =
                        FillManager(
                            mapView,
                            map,
                            style,
                            LocationComponentConstants.SHADOW_LAYER,
                            null,
                        )
                }
            },
            updateMap = { map ->
                // Only update the camera position when it's at the default location to avoid jank
                if (map.cameraPosition.target?.isCloseToOrigin() == true) {
                    val (center, zoom) = mapViewport!!

                    map.cameraPosition =
                        CameraPosition.Builder()
                            .target(center.asMapLibreLatLng())
                            .zoom(zoom)
                            .build()
                }

                if (myLocation != null && map.locationComponent.isLocationComponentActivated) {
                    map.locationComponent.forceLocationUpdate(myLocation!!.asPlatformLocation())

                    if (trackMyLocation.value) {
                        map.cameraPosition =
                            CameraPosition.Builder()
                                .target(myLocation!!.position.latLng.asMapLibreLatLng())
                                .build()
                    }
                }

                map.addCoverageLayerFromTileJson(
                    coverageTileJsonUrl.value,
                    coverageTileJsonLayerIds.value,
                    color =
                        if (isSystemInDarkTheme()) {
                            COVERAGE_COLOR_DARK
                        } else {
                            COVERAGE_COLOR
                        },
                    opacity =
                        if (isSystemInDarkTheme()) {
                            COVERAGE_OPACITY_DARK
                        } else {
                            COVERAGE_OPACITY
                        },
                )

                fillManager.value?.let { createHeatMapFill(it, heatMapTiles.value) }
            },
        )

        Box(
            modifier =
                Modifier.fillMaxSize().padding(16.dp).windowInsetsPadding(WindowInsets.systemBars)
        ) {
            MapSettingsButton(modifier = Modifier.size(32.dp).align(Alignment.TopEnd))

            FilledIconButton(
                modifier = Modifier.size(48.dp).align(Alignment.BottomEnd),
                onClick = {
                    if (
                        context
                            .checkMissingPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
                            .isEmpty()
                    ) {
                        mapViewModel.setShowMyLocation(true)
                        trackMyLocation.value = true
                    } else {
                        showPermissionDialog.value = true
                    }
                },
            ) {
                Icon(
                    painter =
                        if (trackMyLocation.value) {
                            painterResource(id = R.drawable.my_location_24px)
                        } else {
                            painterResource(id = R.drawable.location_searching_24px)
                        },
                    contentDescription = stringResource(id = R.string.show_my_location),
                )
            }
        }
    }
}

private class OnMapMoveBeginListener(private val onMoveBegin: () -> Unit) :
    MapLibreMap.OnMoveListener {
    override fun onMoveBegin(p0: MoveGestureDetector) {
        onMoveBegin()
    }

    override fun onMove(p0: MoveGestureDetector) {}

    override fun onMoveEnd(p0: MoveGestureDetector) {}
}

private const val MAX_COORDINATE_DIFF = 0.00001

private fun LatLng.isCloseToOrigin(): Boolean {
    return abs(latitude) < MAX_COORDINATE_DIFF && abs(longitude) < MAX_COORDINATE_DIFF
}

private fun PositionObservation.asPlatformLocation(): Location {
    return Location("manual").apply {
        this.latitude = position.latitude
        this.longitude = position.longitude
        if (position.accuracy != null) {
            this.accuracy = position.accuracy!!.toFloat()
        }
    }
}

private fun addCoverageLayer(style: Style, layerIds: List<String>, color: String, opacity: Float) {
    for (id in layerIds) {
        val layer = style.getLayer(COVERAGE_LAYER_PREFIX + id)
        if (layer == null) {
            style.addLayer(
                FillLayer(COVERAGE_LAYER_PREFIX + id, COVERAGE_SOURCE_ID).apply {
                    withProperties(
                        PropertyFactory.fillColor(color),
                        PropertyFactory.fillOpacity(opacity),
                    )
                    sourceLayer = id
                }
            )
        } else {
            (layer as? FillLayer)?.sourceLayer = id
        }
    }
}

private fun MapLibreMap.addCoverageLayerFromTileJson(
    tileJsonUrl: String?,
    layerIds: List<String>,
    color: String,
    opacity: Float,
) {
    getStyle { style ->
        if (tileJsonUrl != null) {
            val vectorSource = style.getSource(COVERAGE_SOURCE_ID) as? VectorSource
            if (vectorSource == null || vectorSource.uri != tileJsonUrl) {
                style.removeSource(COVERAGE_SOURCE_ID)
                style.addSource(VectorSource(COVERAGE_SOURCE_ID, tileJsonUrl))
            }

            addCoverageLayer(style, layerIds, color, opacity)
        } else {
            // Remove coverage layers
            style.layers
                .filter { it.id.startsWith(COVERAGE_LAYER_PREFIX) }
                .forEach { style.removeLayer(it) }

            style.removeSource(COVERAGE_SOURCE_ID)
        }
    }
}

private fun createHeatMapFill(
    fillManager: FillManager,
    tiles: Map<String, MapViewModel.HeatMapTile>,
) {
    val updated = mutableSetOf<String>()
    val toDelete = mutableListOf<Fill>()

    fillManager.annotations.forEach { _, fill ->
        val hexKey = fill.data!!.asString

        if (hexKey !in tiles) {
            toDelete += fill
        } else {
            val tile = tiles[hexKey]!!

            val color =
                MapLibreColorUtils.colorToRgbaString(
                    ColorUtils.blendARGB(HEAT_LOW, HEAT_HIGH, tile.heatPct)
                )

            if (color != fill.fillColor) {
                fill.fillColor = color
            }

            updated += hexKey
        }
    }

    fillManager.delete(toDelete)

    tiles
        .filter { it.key !in updated }
        .forEach { (hexKey, tile) ->
            val color = ColorUtils.blendARGB(HEAT_LOW, HEAT_HIGH, tile.heatPct)

            val fillOptions =
                FillOptions()
                    .withData(JsonPrimitive(hexKey))
                    .withFillColor(MapLibreColorUtils.colorToRgbaString(color))
                    .withFillOutlineColor("#00000000")
                    .withLatLngs(listOf(tile.outline.map { LatLng(it.latitude, it.longitude) }))

            fillManager.create(fillOptions)
        }
}
