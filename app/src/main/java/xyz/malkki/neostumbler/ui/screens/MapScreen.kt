package xyz.malkki.neostumbler.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import androidx.annotation.ColorInt
import androidx.collection.forEach
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.JsonPrimitive
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.gestures.MoveGestureDetector
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentConstants
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
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
import xyz.malkki.neostumbler.ui.composables.shared.KeepScreenOn
import xyz.malkki.neostumbler.ui.composables.shared.PermissionsDialog
import xyz.malkki.neostumbler.ui.map.LifecycleAwareMapView
import xyz.malkki.neostumbler.ui.map.setAttributionMargin
import xyz.malkki.neostumbler.ui.map.updateMapStyleIfNeeded
import xyz.malkki.neostumbler.ui.viewmodel.MapViewModel

@ColorInt private const val HEAT_LOW: Int = 0x78d278ff
@ColorInt private const val HEAT_HIGH: Int = 0x78aa00ff

private const val COVERAGE_SOURCE_ID = "coverage-source"
private const val COVERAGE_LAYER_PREFIX = "coverage-layer-"
private const val COVERAGE_COLOR = "#ff8000"
private const val COVERAGE_OPACITY = 0.4f

private const val MIN_ZOOM = 3.0
private const val MAX_ZOOM = 15.0

// This method is long and complex because we are mixing Compose with Views
// FIXME: try to break this into smaller pieces and then remove these suppressions
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun MapScreen(mapViewModel: MapViewModel = koinViewModel<MapViewModel>()) {
    val context = LocalContext.current

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val density = LocalDensity.current

    val showPermissionDialog = rememberSaveable { mutableStateOf(false) }

    val trackMyLocation = rememberSaveable { mutableStateOf(false) }

    val fillManager = remember { mutableStateOf<FillManager?>(null) }

    val httpClient = mapViewModel.httpClient.collectAsState(initial = null)

    val mapTileSourceUrl = mapViewModel.mapTileSourceUrl.collectAsState(initial = null)

    val coverageTileJsonUrl = mapViewModel.coverageTileJsonUrl.collectAsState(initial = null)

    val coverageTileJsonLayerIds =
        mapViewModel.coverageTileJsonLayerIds.collectAsState(initial = emptyList<String>())

    val latestReportPosition = mapViewModel.latestReportPosition.collectAsState(initial = null)

    val heatMapTiles = mapViewModel.heatMapTiles.collectAsState(initial = emptyMap())

    val myLocation =
        mapViewModel.myLocation.collectAsStateWithLifecycle(
            initialValue = null,
            minActiveState = Lifecycle.State.RESUMED,
        )

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
        if (mapTileSourceUrl.value != null && httpClient.value != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapLibre.getInstance(context)
                    HttpRequestUtil.setOkHttpClient(httpClient.value)

                    val mapView = LifecycleAwareMapView(context)
                    mapView.context.registerComponentCallbacks(mapView.componentCallback)

                    mapView.localizeLabelNames()

                    mapView.getMapAsync { map ->
                        map.addOnMoveListener(
                            object : MapLibreMap.OnMoveListener {
                                override fun onMoveBegin(p0: MoveGestureDetector) {
                                    trackMyLocation.value = false
                                }

                                override fun onMove(p0: MoveGestureDetector) {}

                                override fun onMoveEnd(p0: MoveGestureDetector) {}
                            }
                        )

                        map.addOnCameraMoveListener {
                            val mapCenter = map.cameraPosition.target!!.asDomainLatLng()

                            mapViewModel.setMapCenter(mapCenter)
                            mapViewModel.setZoom(map.cameraPosition.zoom)

                            mapViewModel.setMapBounds(
                                minLatitude =
                                    map.projection.visibleRegion.latLngBounds.latitudeSouth,
                                maxLatitude =
                                    map.projection.visibleRegion.latLngBounds.latitudeNorth,
                                minLongitude =
                                    map.projection.visibleRegion.latLngBounds.longitudeWest,
                                maxLongitude =
                                    map.projection.visibleRegion.latLngBounds.longitudeEast,
                            )
                        }

                        map.setMinZoomPreference(MIN_ZOOM)
                        map.setMaxZoomPreference(MAX_ZOOM)

                        map.setAttributionMargin(density)

                        map.uiSettings.isRotateGesturesEnabled = false

                        val styleBuilder = Style.Builder().fromUri(mapTileSourceUrl.value!!)

                        map.setStyle(styleBuilder) { style ->
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
                    }

                    mapView
                },
                update = { mapView ->
                    mapView.lifecycle = lifecycle

                    mapView.getMapAsync { map ->
                        // Call repeatedly in update() because latestReportPosition may not be
                        // available in factory()
                        val target =
                            if (mapViewModel.mapCenter.value.isOrigin()) {
                                latestReportPosition.value
                            } else {
                                mapViewModel.mapCenter.value
                            }
                        map.cameraPosition =
                            CameraPosition.Builder()
                                .target(target?.asMapLibreLatLng())
                                .zoom(mapViewModel.zoom.value)
                                .build()

                        if (
                            myLocation.value != null &&
                                map.locationComponent.isLocationComponentActivated
                        ) {
                            map.locationComponent.forceLocationUpdate(
                                myLocation.value!!.asPlatformLocation()
                            )

                            if (trackMyLocation.value) {
                                map.cameraPosition =
                                    CameraPosition.Builder()
                                        .target(
                                            myLocation.value!!.position.latLng.asMapLibreLatLng()
                                        )
                                        .build()
                            }
                        }

                        mapTileSourceUrl.value?.let { map.updateMapStyleIfNeeded(it) }

                        addCoverage(map, coverageTileJsonUrl.value, coverageTileJsonLayerIds.value)
                    }

                    fillManager.value?.let { createHeatMapFill(it, heatMapTiles.value) }
                },
                onRelease = { view ->
                    view.context.unregisterComponentCallbacks(view.componentCallback)

                    view.getMapAsync { map ->
                        // No permission is needed to disable the location component
                        @SuppressLint("MissingPermission")
                        try {
                            // The location component has to be disabled before destroying the view
                            map.locationComponent.isLocationComponentEnabled = false
                        } catch (_: IllegalStateException) {
                            // And if disabling the location component fails, it's safe to destroy
                            // the view
                        }
                    }

                    view.lifecycle = null
                },
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                            rememberVectorPainter(Icons.Default.MyLocation)
                        } else {
                            rememberVectorPainter(Icons.Default.LocationSearching)
                        },
                    contentDescription = stringResource(id = R.string.show_my_location),
                )
            }
        }
    }
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

private fun addCoverageLayer(style: Style, layerIds: List<String>) {
    for (id in layerIds) {
        val layer = style.getLayer(COVERAGE_LAYER_PREFIX + id)
        if (layer == null) {
            style.addLayer(
                FillLayer(COVERAGE_LAYER_PREFIX + id, COVERAGE_SOURCE_ID).apply {
                    withProperties(
                        PropertyFactory.fillColor(COVERAGE_COLOR),
                        PropertyFactory.fillOpacity(COVERAGE_OPACITY),
                    )
                    sourceLayer = id
                }
            )
        } else {
            (layer as? FillLayer)?.sourceLayer = id
        }
    }
}

private fun addCoverage(mapLibreMap: MapLibreMap, tileJsonUrl: String?, layerIds: List<String>) {
    mapLibreMap.getStyle { style ->
        if (tileJsonUrl != null) {
            val vectorSource = style.getSource(COVERAGE_SOURCE_ID) as? VectorSource
            if (vectorSource == null || vectorSource.uri != tileJsonUrl) {
                style.removeSource(COVERAGE_SOURCE_ID)
                style.addSource(VectorSource(COVERAGE_SOURCE_ID, tileJsonUrl))
            }

            addCoverageLayer(style, layerIds)
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
