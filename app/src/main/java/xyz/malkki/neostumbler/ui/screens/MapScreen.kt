package xyz.malkki.neostumbler.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.gestures.MoveGestureDetector
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentConstants
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.plugins.annotation.FillManager
import org.maplibre.android.plugins.annotation.FillOptions
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.ui.composables.KeepScreenOn
import xyz.malkki.neostumbler.ui.composables.PermissionsDialog
import xyz.malkki.neostumbler.ui.viewmodel.MapViewModel
import xyz.malkki.neostumbler.ui.viewmodel.MapViewModel.MapTileSource

private val HEAT_LOW = ColorUtils.setAlphaComponent(0xd278ff, 120)
private val HEAT_HIGH = ColorUtils.setAlphaComponent(0xaa00ff, 120)

@Composable
fun MapScreen(mapViewModel: MapViewModel = viewModel()) {
    val context = LocalContext.current

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val density = LocalDensity.current

    val showPermissionDialog = rememberSaveable {
        mutableStateOf(false)
    }

    val trackMyLocation = rememberSaveable {
        mutableStateOf(false)
    }

    val loadedStyle = remember {
        mutableStateOf<MapViewModel.MapStyle?>(null)
    }

    val fillManager = remember {
        mutableStateOf<FillManager?>(null)
    }

    val httpClient = mapViewModel.httpClient.collectAsState(initial = null)

    val selectedMapTileSource = mapViewModel.mapTileSource.collectAsState(initial = null)

    val mapStyle = mapViewModel.mapStyle.collectAsState(initial = null)

    val latestReportPosition = mapViewModel.latestReportPosition.collectAsState(initial = null)

    val heatMapTiles = mapViewModel.heatMapTiles.collectAsState(initial = emptyList())

    val myLocation = mapViewModel.myLocation.collectAsStateWithLifecycle(initialValue = null, minActiveState = Lifecycle.State.RESUMED)

    if (showPermissionDialog.value) {
        PermissionsDialog(
            missingPermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION),
            permissionRationales = mapOf(
                Manifest.permission.ACCESS_FINE_LOCATION to stringResource(R.string.permission_rationale_location_map)
            ),
            onPermissionsGranted = {
                showPermissionDialog.value = false

                val hasPermission = context.checkMissingPermissions(Manifest.permission.ACCESS_COARSE_LOCATION).isEmpty()

                mapViewModel.setShowMyLocation(hasPermission)
                trackMyLocation.value = hasPermission
            }
        )
    }

    KeepScreenOn()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (mapStyle.value != null && httpClient.value != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapLibre.getInstance(context)
                    HttpRequestUtil.setOkHttpClient(httpClient.value)

                    val mapView = LifecycleAwareMap(context)
                    mapView.getMapAsync { map ->
                        map.addOnMoveListener(object : MapLibreMap.OnMoveListener {
                            override fun onMoveBegin(p0: MoveGestureDetector) {
                                trackMyLocation.value = false
                            }

                            override fun onMove(p0: MoveGestureDetector) {}

                            override fun onMoveEnd(p0: MoveGestureDetector) {}
                        })

                        map.setMinZoomPreference(3.0)
                        map.setMaxZoomPreference(15.0)

                        val attributionMargin = density.run { 8.dp.roundToPx() }

                        map.uiSettings.isLogoEnabled = false
                        map.uiSettings.setAttributionMargins(attributionMargin, 0, 0, attributionMargin)
                        map.uiSettings.isAttributionEnabled = true

                        map.uiSettings.isRotateGesturesEnabled = false

                        map.addOnCameraMoveListener(object : MapLibreMap.OnCameraMoveListener {
                            override fun onCameraMove() {
                                val mapCenter = xyz.malkki.neostumbler.common.LatLng(map.cameraPosition.target!!.latitude, map.cameraPosition.target!!.longitude)

                                mapViewModel.setMapCenter(mapCenter)
                                mapViewModel.setZoom(map.cameraPosition.zoom)

                                mapViewModel.setMapBounds(
                                    minLatitude = map.projection.visibleRegion.latLngBounds.latitudeSouth,
                                    maxLatitude = map.projection.visibleRegion.latLngBounds.latitudeNorth,
                                    minLongitude = map.projection.visibleRegion.latLngBounds.longitudeWest,
                                    maxLongitude = map.projection.visibleRegion.latLngBounds.longitudeEast
                                )
                            }
                        })

                        val styleBuilder = Style.Builder().apply {
                            if (mapStyle.value!!.styleJson != null) {
                                fromJson(mapStyle.value!!.styleJson!!)
                            } else {
                                fromUri(mapStyle.value!!.styleUrl!!)
                            }
                        }

                        map.setStyle(styleBuilder) { style ->
                            map.locationComponent.activateLocationComponent(
                                LocationComponentActivationOptions.builder(context, style)
                                    //Set location engine to null, because we provide locations by ourself
                                    .locationEngine(null)
                                    .useDefaultLocationEngine(false)
                                    .build()
                            )
                            @SuppressLint("MissingPermission")
                            map.locationComponent.isLocationComponentEnabled = true
                            map.locationComponent.renderMode = RenderMode.COMPASS

                            fillManager.value = FillManager(mapView, map, style, LocationComponentConstants.SHADOW_LAYER, null)
                        }
                    }

                    mapView
                },
                update = { mapView ->
                    mapView.lifecycle = lifecycle

                    mapView.getMapAsync { map ->
                        if (map.cameraPosition.target == null || (map.cameraPosition.target?.latitude == 0.0 && map.cameraPosition.target?.longitude == 0.0)) {
                            if (latestReportPosition.value != null) {
                                map.cameraPosition = CameraPosition.Builder()
                                    .target(latestReportPosition.value?.let { LatLng(it.latitude, it.longitude) })
                                    .zoom(10.0)
                                    .build()
                            } else {
                                map.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(mapViewModel.mapCenter.value.latitude, mapViewModel.mapCenter.value.longitude))
                                    .zoom(mapViewModel.zoom.value)
                                    .build()
                            }
                        }

                        if (myLocation.value != null) {
                            map.locationComponent.forceLocationUpdate(myLocation.value!!.location)

                            if (trackMyLocation.value) {
                                map.cameraPosition = CameraPosition.Builder().target(LatLng(myLocation.value!!.location.latitude, myLocation.value!!.location.longitude)).build()
                            }
                        }

                        //Ugly, but we don't want to update the map style unless it has actually changed
                        //TODO: think about a better way to do this
                        if (map.style != null) {
                            if (mapStyle.value!!.styleUrl != null && map.style!!.uri != mapStyle.value!!.styleUrl) {
                                map.setStyle(Style.Builder().fromUri(mapStyle.value!!.styleUrl!!))
                            } else if (mapStyle.value!!.styleJson != null && map.style!!.json != mapStyle.value!!.styleJson) {
                                map.setStyle(Style.Builder().fromJson(mapStyle.value!!.styleJson!!))
                            }
                        }
                    }

                    fillManager.value?.let {
                        it.deleteAll()

                        it.create(createHeatMapFill(heatMapTiles.value))
                    }
                },
                onRelease = { view ->
                    view.lifecycle = null
                }
            )
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            if (selectedMapTileSource.value != null) {
                MapTileSourceButton(
                    modifier = Modifier.size(32.dp).align(Alignment.TopEnd),
                    selectedMapTileSource = selectedMapTileSource.value!!,
                    onMapTileSourceSelected = { mapViewModel.setMapTileSource(it) }
                )
            }

            FilledIconButton(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.BottomEnd),
                onClick = {
                    if (context.checkMissingPermissions(Manifest.permission.ACCESS_FINE_LOCATION).isEmpty()) {
                        mapViewModel.setShowMyLocation(true)
                        trackMyLocation.value = true
                    } else {
                        showPermissionDialog.value = true
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.my_location_24),
                    contentDescription = stringResource(id = R.string.show_my_location)
                )
            }
        }
    }
}

private fun createHeatMapFill(tiles: Collection<MapViewModel.HeatMapTile>): List<FillOptions> {
    return tiles.map { tile ->
        val color = ColorUtils.blendARGB(HEAT_LOW, HEAT_HIGH, tile.heatPct)

        FillOptions()
            .withFillColor(org.maplibre.android.utils.ColorUtils.colorToRgbaString(color))
            .withFillOutlineColor(org.maplibre.android.utils.ColorUtils.colorToRgbaString(ColorUtils.setAlphaComponent(0, 0)))
            .withLatLngs(listOf(tile.outline.map {
                LatLng(it.latitude, it.longitude)
            }))
    }
}

@Composable
private fun MapTileSourceButton(modifier: Modifier, selectedMapTileSource: MapTileSource, onMapTileSourceSelected: (MapTileSource) -> Unit) {
    val dialogOpen = rememberSaveable { mutableStateOf(false) }

    if (dialogOpen.value) {
        BasicAlertDialog(
            onDismissRequest = {
                dialogOpen.value = false
            }
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.small,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        style = MaterialTheme.typography.titleLarge,
                        text = stringResource(id = R.string.map_tile_source),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .selectableGroup()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MapTileSource.entries.forEach { mapTileSource ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .defaultMinSize(minHeight = 36.dp)
                                    .selectable(
                                        selected = mapTileSource == selectedMapTileSource,
                                        onClick = {
                                            onMapTileSourceSelected(mapTileSource)

                                            dialogOpen.value = false
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(top = 4.dp),
                                    selected = mapTileSource == selectedMapTileSource,
                                    onClick = null
                                )

                                Text(
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                        .padding(start = 16.dp),
                                    text = mapTileSource.title,
                                    style = MaterialTheme.typography.bodyMedium.merge()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    FilledTonalIconButton(
        modifier = modifier,
        onClick = {
            dialogOpen.value = true
        },
        colors = IconButtonDefaults.filledTonalIconButtonColors()
    ) {
        Icon(
            painter = painterResource(id = R.drawable.layers_18),
            contentDescription = stringResource(id = R.string.map_tile_source)
        )
    }
}

private class LifecycleAwareMap(context: Context) : MapView(context) {
    var lifecycle: Lifecycle? = null
        set(value) {
            field?.removeObserver(observer)
            value?.addObserver(observer)
            field = value
        }

    private val observer = LifecycleEventObserver { source, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                onResume()
            }
            Lifecycle.Event.ON_PAUSE -> {
                onPause()
            }
            Lifecycle.Event.ON_START -> {
                onStart()
            }
            Lifecycle.Event.ON_STOP -> {
                onStop()
            }
            Lifecycle.Event.ON_DESTROY -> {
                onDestroy()
            }
            else -> {}
        }
    }
}
