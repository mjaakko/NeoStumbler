package xyz.malkki.neostumbler.ui.composables.shared

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.utils.ColorUtils
import org.maplibre.geojson.Point
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.data.location.LocationSource
import xyz.malkki.neostumbler.domain.asDomainLatLng
import xyz.malkki.neostumbler.domain.asMapLibreLatLng
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.geography.LatLng
import xyz.malkki.neostumbler.utils.maplibre.needsRecreation

@Composable
fun AreaPickerDialog(
    title: String,
    positiveButtonText: String,
    negativeButtonText: String = stringResource(R.string.cancel),
    onAreaSelected: (Pair<LatLng, Double>?) -> Unit,
) {
    val circle = remember { mutableStateOf(LatLng(0.0, 0.0) to 0.0) }

    BasicAlertDialog(onDismissRequest = { onAreaSelected.invoke(null) }) {
        Surface(
            modifier = Modifier.sizeIn(maxWidth = 400.dp).fillMaxWidth().height(400.dp),
            shape = AlertDialogDefaults.shape,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(all = 24.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1.0f)) {
                    AreaPickerMap(onCircleUpdated = { circle.value = it })
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.align(Alignment.End)) {
                    TextButton(onClick = { onAreaSelected.invoke(null) }) {
                        Text(text = negativeButtonText)
                    }

                    TextButton(
                        onClick = { onAreaSelected.invoke(circle.value) },
                        enabled =
                            circle.value.first.latitude != 0.0 ||
                                circle.value.first.longitude != 0.0,
                    ) {
                        Text(text = positiveButtonText)
                    }
                }
            }
        }
    }
}

// Use 90% of available space
private const val CIRCLE_SCALE_FACTOR = 0.9f

private const val MIN_ZOOM = 6.0
private const val MAX_ZOOM = 15.0

private const val DEFAULT_ZOOM = 12.0

@Composable
private fun getCurrentLocation(
    context: Context = LocalContext.current,
    locationSource: LocationSource = koinInject(),
): State<LatLng?> =
    produceState(initialValue = null) {
        value =
            if (
                context.checkMissingPermissions(Manifest.permission.ACCESS_FINE_LOCATION).isEmpty()
            ) {
                @SuppressLint("MissingPermission")
                locationSource
                    .getLocations(1.seconds, usePassiveProvider = false)
                    .first()
                    .position
                    .latLng
            } else {
                // If we don't have location permission, just show any location to avoid crashing
                LatLng(0.0, 0.0)
            }
    }

private const val CIRCLE_FILL_OPACITY = 0.2f
private const val CIRCLE_STROKE_OPACITY = 0.9f
private const val CIRCLE_STROKE_WIDTH = 2f

@Composable
fun AreaPickerMap(onCircleUpdated: (Pair<LatLng, Double>) -> Unit) {
    val density = LocalDensity.current

    val currentLocation = getCurrentLocation()

    if (currentLocation.value == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(R.string.waiting_for_location))
        }
    } else {
        var geoJsonSource by remember { mutableStateOf(GeoJsonSource("circle")) }

        ComposableMap(
            modifier = Modifier.fillMaxSize(),
            onInit = { map, _ ->
                val radius =
                    density.run { (CIRCLE_SCALE_FACTOR * minOf(map.width, map.height)).toDp() / 2 }

                map.addOnCameraMoveListener {
                    val newMapCenter = map.cameraPosition.target!!.asDomainLatLng()

                    val radiusMeters =
                        radius.value *
                            map.projection.getMetersPerPixelAtLatitude(newMapCenter.latitude)

                    onCircleUpdated(newMapCenter to radiusMeters)

                    geoJsonSource.setGeoJson(
                        Point.fromLngLat(newMapCenter.longitude, newMapCenter.latitude)
                    )

                    map.getStyle { style ->
                        style.layers
                            .find { it.id == "circle-layer" }
                            ?.setProperties(PropertyFactory.circleRadius(radius.value))
                    }
                }

                val cameraPos = currentLocation.value!!.asMapLibreLatLng()

                map.cameraPosition =
                    CameraPosition.Builder().target(cameraPos).zoom(DEFAULT_ZOOM).build()

                map.setMinZoomPreference(MIN_ZOOM)
                map.setMaxZoomPreference(MAX_ZOOM)

                map.uiSettings.isRotateGesturesEnabled = false
            },
            onStyleUpdated = { style ->
                if (geoJsonSource.needsRecreation()) {
                    geoJsonSource = GeoJsonSource("circle")
                }

                style.addSource(geoJsonSource)

                style.addLayer(
                    CircleLayer("circle-layer", geoJsonSource.id).apply {
                        setProperties(
                            PropertyFactory.circleColor(ColorUtils.colorToRgbaString(Color.CYAN)),
                            PropertyFactory.circleOpacity(CIRCLE_FILL_OPACITY),
                            PropertyFactory.circleStrokeColor(
                                ColorUtils.colorToRgbaString(Color.CYAN)
                            ),
                            PropertyFactory.circleStrokeOpacity(CIRCLE_STROKE_OPACITY),
                            PropertyFactory.circleStrokeWidth(CIRCLE_STROKE_WIDTH),
                        )
                    }
                )
            },
            updateMap = { _ -> },
        )
    }
}
