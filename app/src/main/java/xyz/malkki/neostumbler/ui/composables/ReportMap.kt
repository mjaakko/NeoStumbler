package xyz.malkki.neostumbler.ui.composables

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import org.geohex.geohex4j.GeoHex
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Polygon
import timber.log.Timber
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.common.LatLng
import xyz.malkki.neostumbler.db.dao.ReportDao

//Number of reports needed for max heat color
private const val MAX_HEAT = 15

private val HEAT_LOW = ColorUtils.setAlphaComponent(0xd278ff, 150)
private val HEAT_HIGH = ColorUtils.setAlphaComponent(0xaa00ff, 150)

private data class HeatMapTile(val geohex: String, val heat: Int)

private fun getHeatMapTiles(reportDao: ReportDao): LiveData<List<HeatMapTile>> = reportDao.getAllReportsWithLocation()
    .distinctUntilChanged()
    .map { reportsWithLocation ->
        reportsWithLocation
            .map { reportWithLocation ->
                GeoHex.encode(reportWithLocation.latitude, reportWithLocation.longitude, 9)
            }
            .groupingBy { it }
            .eachCount()
            .map {
                HeatMapTile(it.key, it.value)
            }
    }

/**
 * Sets map to the specified location if the map has not been moved yet
 */
private fun MapView.setPositionIfNotMoved(latLng: LatLng) {
    if (mapCenter.latitude == 0.0 && mapCenter.longitude == 0.0) {
        controller.setCenter(GeoPoint(latLng.latitude, latLng.longitude))
        controller.setZoom(10.0)
    }
}

@Composable
fun ReportMap() {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val reportDb = (LocalContext.current.applicationContext as StumblerApplication).reportDb

    val latestPosition = produceState<LatLng?>(initialValue = null, producer = {
        value = reportDb.positionDao().getLatestPosition()
    })

    val heatMapTiles = getHeatMapTiles(reportDb.reportDao()).observeAsState(
        initial = emptyList()
    )

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
            map.maxZoomLevel = 15.0
            map.minZoomLevel = 3.0
            //Add bounds so that user does not move outside of the area where map tiles are available
            //Latitude range is slightly reduced to avoid displaying blank tiles
            map.setScrollableAreaLimitLatitude(MapView.getTileSystem().maxLatitude - 0.3, MapView.getTileSystem().minLatitude + 0.3, 0)
            map.overlays.add(CopyrightOverlay(context));
            if (latestPosition.value != null) {
                map.setPositionIfNotMoved(latestPosition.value!!)
            } else {
                map.controller.setZoom(5.0)
            }

            map
        },
        update = { view ->
            view.lifecycle = lifecycle

            try {
                latestPosition.value?.let { view.setPositionIfNotMoved(it) }

                val overlay = FolderOverlay()
                overlay.name = "heatmap"

                heatMapTiles.value
                    .map {
                        val heatPct = (it.heat.toFloat() / MAX_HEAT).coerceAtMost(1.0f)

                        val color = ColorUtils.blendARGB(HEAT_LOW, HEAT_HIGH, heatPct)

                        val polygon = Polygon(view)
                        polygon.fillPaint.color = color
                        polygon.outlinePaint.color = ColorUtils.setAlphaComponent(0, 0)
                        polygon.outlinePaint.strokeWidth = 0f
                        //Return with click listener to disable info window
                        polygon.setOnClickListener { _, _, _ -> false }

                        polygon.points = GeoHex.getZoneByCode(it.geohex).hexCoords.map { coord ->
                            GeoPoint(coord.lat, coord.lon)
                        }

                        polygon
                    }
                    .forEach(overlay::add)

                view.overlayManager.removeIf {
                    it is FolderOverlay && it.name == "heatmap"
                }
                view.overlayManager.add(overlay)

                view.invalidate()
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

