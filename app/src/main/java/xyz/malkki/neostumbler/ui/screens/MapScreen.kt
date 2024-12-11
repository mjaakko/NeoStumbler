package xyz.malkki.neostumbler.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.DirectedLocationOverlay
import timber.log.Timber
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.ui.composables.KeepScreenOn
import xyz.malkki.neostumbler.ui.composables.PermissionsDialog
import xyz.malkki.neostumbler.ui.viewmodel.MapViewModel
import kotlin.math.roundToInt


private val HEAT_LOW = ColorUtils.setAlphaComponent(0xd278ff, 120)
private val HEAT_HIGH = ColorUtils.setAlphaComponent(0xaa00ff, 120)

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapScreen(mapViewModel: MapViewModel = viewModel()) {
    val context = LocalContext.current

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val showPermissionDialog = rememberSaveable {
        mutableStateOf(false)
    }

    val trackMyLocation = rememberSaveable {
        mutableStateOf(false)
    }

    val latestReportPosition = mapViewModel.latestReportPosition.observeAsState(initial = null)

    val heatMapTiles = mapViewModel.heatMapTiles.observeAsState(initial = emptyList())

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
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))

                //https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library-(Java)

                val map = LifecycleAwareMap(context)
                map.setTileSource(TileSourceFactory.MAPNIK)
                map.setMultiTouchControls(true)

                map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                map.isTilesScaledToDpi = true
                map.isVerticalMapRepetitionEnabled = false
                map.maxZoomLevel = 16.0
                map.minZoomLevel = 3.0
                //Add bounds so that user does not move outside of the area where map tiles are available
                //Latitude range is slightly reduced to avoid displaying blank tiles
                map.setScrollableAreaLimitLatitude(MapView.getTileSystem().maxLatitude - 0.3, MapView.getTileSystem().minLatitude + 0.3, 0)

                map.overlays.add(CopyrightOverlay(context))
                map.overlays.add(DirectedLocationOverlay(context))

                map.setOnTouchListener { _, _ ->
                    trackMyLocation.value = false

                    false
                }

                map.addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent): Boolean {
                        mapViewModel.setMapCenter(event.source.mapCenter)
                        mapViewModel.setMapBounds(
                            minLatitude = event.source.boundingBox.latSouth,
                            minLongitude = event.source.boundingBox.lonWest,
                            maxLatitude = event.source.boundingBox.latNorth,
                            maxLongitude = event.source.boundingBox.lonEast
                        )

                        return false
                    }

                    override fun onZoom(event: ZoomEvent): Boolean {
                        mapViewModel.setZoom(event.source.zoomLevelDouble)
                        mapViewModel.setMapBounds(
                            minLatitude = event.source.boundingBox.latSouth,
                            minLongitude = event.source.boundingBox.lonWest,
                            maxLatitude = event.source.boundingBox.latNorth,
                            maxLongitude = event.source.boundingBox.lonEast
                        )

                        return false
                    }
                })

                map
            },
            update = { map ->
                map.lifecycle = lifecycle

                try {
                    if (myLocation.value == null || !trackMyLocation.value) {
                        val center = mapViewModel.mapCenter.value
                        val zoom = mapViewModel.zoom.value

                        if (center == null || center.latitude == 0.0 && center.longitude == 0.0) {
                            if (latestReportPosition.value != null) {
                                //Set map view to the latest report position if there is no saved view
                                map.controller.setCenter(GeoPoint(latestReportPosition.value!!.latitude, latestReportPosition.value!!.longitude))
                                map.controller.setZoom(10.0)
                            } else {
                                //Otherwise just decrease the zoom level
                                map.controller.setZoom(5.0)
                            }
                        } else {
                            map.controller.setCenter(center)
                            map.controller.setZoom(zoom!!)
                        }
                    }

                    myLocation.value?.let { locationWithSource ->
                        val locationOverlay = map.overlays.find { it is DirectedLocationOverlay }!! as DirectedLocationOverlay

                        val geopoint = GeoPoint(locationWithSource.location.latitude, locationWithSource.location.longitude)

                        locationOverlay.location = geopoint
                        if (locationWithSource.location.hasBearing()) {
                            locationOverlay.setBearing(locationWithSource.location.bearing)
                        }

                        if (locationWithSource.location.hasAccuracy()) {
                            locationOverlay.setShowAccuracy(true)
                            locationOverlay.setAccuracy(locationWithSource.location.accuracy.roundToInt())
                        } else {
                            locationOverlay.setShowAccuracy(false)
                        }

                        if (trackMyLocation.value) {
                            map.controller.setCenter(geopoint)
                        }
                    }

                    val overlay = createHeatMapOverlay(map, heatMapTiles.value)

                    map.overlayManager.removeIf {
                        it is FolderOverlay && it.name == "heatmap"
                    }
                    map.overlayManager.add(overlay)

                    map.invalidate()
                } catch (npe: NullPointerException) {
                    //Due to a bug in OSMDroid, updating the map can sometimes throw null pointer exception
                    //To avoid crashing the app, just ignore it
                    //The null pointer exception seems to only happen when the map is being removed from Composable tree
                    Timber.w(npe, "Map update failed due to a NullPointerException")
                }
            },
            onRelease = { view ->
                view.lifecycle = null
            }
        )

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

private fun createHeatMapOverlay(mapView: MapView, heatMapTiles: Collection<MapViewModel.HeatMapTile>): FolderOverlay {
    return FolderOverlay().apply {
        name = "heatmap"

        heatMapTiles
            .map { heatMapTile ->
                val color = ColorUtils.blendARGB(HEAT_LOW, HEAT_HIGH, heatMapTile.heatPct)

                val polygon = Polygon(mapView)
                polygon.fillPaint.color = color
                polygon.outlinePaint.color = ColorUtils.setAlphaComponent(0, 0)
                polygon.outlinePaint.strokeWidth = 0f
                //Return with click listener to disable info window
                polygon.setOnClickListener { _, _, _ -> false }

                polygon.points = heatMapTile.outline

                polygon
            }
            .forEach(::add)
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
            else -> {}
        }
    }
}
