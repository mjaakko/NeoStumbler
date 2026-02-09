package xyz.malkki.neostumbler.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.ColorInt
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentConstants
import org.maplibre.android.location.OnCameraTrackingChangedListener
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyValue
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.VectorSource
import org.maplibre.geojson.FeatureCollection
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.domain.asDomainLatLng
import xyz.malkki.neostumbler.domain.asMapLibreLatLng
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.ui.composables.map.MapSettingsButton
import xyz.malkki.neostumbler.ui.composables.shared.ComposableMap
import xyz.malkki.neostumbler.ui.composables.shared.KeepScreenOn
import xyz.malkki.neostumbler.ui.composables.shared.PermissionsDialog
import xyz.malkki.neostumbler.ui.viewmodel.MapViewModel
import xyz.malkki.neostumbler.utils.maplibre.FlowLocationEngine
import xyz.malkki.neostumbler.utils.maplibre.needsRecreation

@ColorInt private const val HEAT_LOW: Int = 0x78d278ff
@ColorInt private const val HEAT_HIGH: Int = 0x78aa00ff

@ColorInt private const val HEAT_LOW_DARK: Int = 0x65de9cff
@ColorInt private const val HEAT_HIGH_DARK: Int = 0x65bb45ff

private const val COVERAGE_SOURCE_ID = "coverage-source"
private const val COVERAGE_LAYER_PREFIX = "coverage-layer-"

private const val COVERAGE_COLOR = "#ff8000"
private const val COVERAGE_COLOR_DARK = "#ffbb00"
private const val COVERAGE_OPACITY = 0.4f
private const val COVERAGE_OPACITY_DARK = 0.25f

private const val MIN_ZOOM = 3.0
private const val MAX_ZOOM = 15.0

private const val HEATMAP_SOURCE_ID = "neostumbler-heat-map-source"
private const val HEATMAP_LAYER_ID = "neostumbler-heat-map"

// This method is long because we are mixing Compose with Views
// FIXME: try to break this into smaller pieces and then remove these suppressions
@Suppress("LongMethod")
@Composable
fun MapScreen(mapViewModel: MapViewModel = koinViewModel<MapViewModel>()) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }

    var trackMyLocation by rememberSaveable { mutableStateOf(false) }

    val coverageTileJsonUrl by mapViewModel.coverageTileJsonUrl.collectAsStateWithLifecycle()

    val coverageTileJsonLayerIds by
        mapViewModel.coverageTileJsonLayerIds.collectAsStateWithLifecycle()

    val darkMode = isSystemInDarkTheme()

    var geoJsonSource by remember { mutableStateOf(GeoJsonSource(HEATMAP_SOURCE_ID)) }

    LaunchedEffect(geoJsonSource) {
        mapViewModel.heatMapPolygons.collect { features ->
            geoJsonSource.setGeoJson(FeatureCollection.fromFeatures(features))
        }
    }

    val mapViewport by mapViewModel.mapViewport.collectAsStateWithLifecycle()

    if (showPermissionDialog) {
        MapPermissionsDialog(
            onPermissionsGranted = { permissionGranted ->
                showPermissionDialog = false

                mapViewModel.setShowMyLocation(permissionGranted)
                trackMyLocation = permissionGranted
            }
        )
    }

    KeepScreenOn()

    Box(modifier = Modifier.fillMaxSize()) {
        ComposableMap(
            modifier = Modifier.fillMaxSize(),
            onInit = { map, _ ->
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
                    map.setupLocationComponent(
                        context,
                        trackMyLocation = trackMyLocation,
                        locationEngine =
                            FlowLocationEngine(
                                positionFlow = mapViewModel.myLocation,
                                coroutineScope = coroutineScope,
                            ),
                        onCameraTrackingDismissed = { trackMyLocation = false },
                    )
                }
            },
            onStyleUpdated = { style ->
                if (geoJsonSource.needsRecreation()) {
                    geoJsonSource = GeoJsonSource(HEATMAP_SOURCE_ID)
                }

                if (style.getSource(HEATMAP_SOURCE_ID) == null) {
                    style.addSource(geoJsonSource)

                    style.addLayerBelow(
                        FillLayer(HEATMAP_LAYER_ID, geoJsonSource.id),
                        LocationComponentConstants.SHADOW_LAYER,
                    )
                }
            },
            updateMap = { map ->
                // Only update the camera position when it's at the default location to avoid jank
                if (map.cameraPosition.target?.isCloseToOrigin() == true) {
                    map.cameraPosition =
                        CameraPosition.Builder()
                            .target(mapViewport.first.asMapLibreLatLng())
                            .zoom(mapViewport.second)
                            .build()
                }

                if (map.locationComponent.isLocationComponentActivated) {
                    if (
                        trackMyLocation &&
                            map.locationComponent.cameraMode != CameraMode.TRACKING_GPS_NORTH
                    ) {
                        map.locationComponent.cameraMode = CameraMode.TRACKING_GPS_NORTH
                    }
                }

                map.addCoverageLayerFromTileJson(
                    tileJsonUrl = coverageTileJsonUrl,
                    layerIds = coverageTileJsonLayerIds,
                    darkMode = darkMode,
                )

                map.getStyle { style ->
                    style.layers
                        .find { it.id == HEATMAP_LAYER_ID }
                        ?.setProperties(getHeatMapFillColor(darkMode))
                }
            },
        )

        Box(
            modifier =
                Modifier.fillMaxSize().padding(16.dp).windowInsetsPadding(WindowInsets.systemBars)
        ) {
            MapSettingsButton(modifier = Modifier.size(32.dp).align(Alignment.TopEnd))

            TrackMyLocationButton(
                modifier = Modifier.size(48.dp).align(Alignment.BottomEnd),
                trackMyLocation = trackMyLocation,
                onClick = {
                    if (
                        context
                            .checkMissingPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
                            .isEmpty()
                    ) {
                        mapViewModel.setShowMyLocation(true)
                        trackMyLocation = true
                    } else {
                        showPermissionDialog = true
                    }
                },
            )
        }
    }
}

@Composable
private fun MapPermissionsDialog(onPermissionsGranted: (Boolean) -> Unit) {
    val context = LocalContext.current

    PermissionsDialog(
        missingPermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION),
        permissionRationales =
            mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to
                    stringResource(R.string.permission_rationale_location_map)
            ),
        onPermissionsGranted = {
            val hasPermission =
                context
                    .checkMissingPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
                    .isEmpty()

            onPermissionsGranted(hasPermission)
        },
    )
}

@Composable
private fun TrackMyLocationButton(
    modifier: Modifier,
    trackMyLocation: Boolean,
    onClick: () -> Unit,
) {
    FilledIconButton(modifier = modifier, onClick = onClick) {
        Icon(
            painter =
                if (trackMyLocation) {
                    painterResource(id = R.drawable.my_location_24px)
                } else {
                    painterResource(id = R.drawable.location_searching_24px)
                },
            contentDescription = stringResource(id = R.string.show_my_location),
        )
    }
}

private fun getHeatMapFillColor(darkMode: Boolean): PropertyValue<Expression> {
    return PropertyFactory.fillColor(
        Expression.interpolate(
            Expression.linear(),
            Expression.get("pct"),
            Expression.stop(
                0.0,
                if (darkMode) {
                    Expression.color(HEAT_LOW_DARK)
                } else {
                    Expression.color(HEAT_LOW)
                },
            ),
            Expression.stop(
                1.0,
                if (darkMode) {
                    Expression.color(HEAT_HIGH_DARK)
                } else {
                    Expression.color(HEAT_HIGH)
                },
            ),
        )
    )
}

private const val MAX_COORDINATE_DIFF = 0.00001

private fun LatLng.isCloseToOrigin(): Boolean {
    return abs(latitude) < MAX_COORDINATE_DIFF && abs(longitude) < MAX_COORDINATE_DIFF
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
    darkMode: Boolean,
) {
    val color =
        if (darkMode) {
            COVERAGE_COLOR_DARK
        } else {
            COVERAGE_COLOR
        }
    val opacity =
        if (darkMode) {
            COVERAGE_OPACITY_DARK
        } else {
            COVERAGE_OPACITY
        }

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

private fun MapLibreMap.setupLocationComponent(
    context: Context,
    trackMyLocation: Boolean,
    locationEngine: LocationEngine,
    onCameraTrackingDismissed: () -> Unit,
) {
    locationComponent.activateLocationComponent(
        LocationComponentActivationOptions.builder(context, style!!)
            .locationEngine(locationEngine)
            .useDefaultLocationEngine(false)
            .useSpecializedLocationLayer(true)
            .build()
    )
    @SuppressLint("MissingPermission")
    locationComponent.isLocationComponentEnabled = true
    locationComponent.renderMode = RenderMode.COMPASS
    locationComponent.cameraMode =
        if (trackMyLocation) {
            CameraMode.TRACKING_GPS_NORTH
        } else {
            CameraMode.NONE
        }
    locationComponent.addOnCameraTrackingChangedListener(
        object : OnCameraTrackingChangedListener {
            override fun onCameraTrackingDismissed() {
                onCameraTrackingDismissed()
            }

            override fun onCameraTrackingChanged(p0: Int) {}
        }
    )
}
