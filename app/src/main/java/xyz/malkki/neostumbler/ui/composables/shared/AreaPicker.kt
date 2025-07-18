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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.Call
import org.koin.compose.koinInject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.plugins.annotation.Circle
import org.maplibre.android.plugins.annotation.CircleManager
import org.maplibre.android.plugins.annotation.CircleOptions
import org.maplibre.android.utils.ColorUtils
import xyz.malkki.neostumbler.PREFERENCES
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.location.LocationSource
import xyz.malkki.neostumbler.domain.asDomainLatLng
import xyz.malkki.neostumbler.domain.asMapLibreLatLng
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.get
import xyz.malkki.neostumbler.geography.LatLng
import xyz.malkki.neostumbler.ui.map.LifecycleAwareMapView
import xyz.malkki.neostumbler.ui.map.MapTileSource
import xyz.malkki.neostumbler.ui.map.setAttributionMargin
import xyz.malkki.neostumbler.ui.map.updateMapStyleIfNeeded

private fun DataStore<Preferences>.mapStyleUrl(): Flow<String> =
    data.map { prefs ->
        val tileSource =
            prefs.get<MapTileSource>(PreferenceKeys.MAP_TILE_SOURCE) ?: MapTileSource.DEFAULT

        if (tileSource == MapTileSource.CUSTOM) {
            prefs.get(stringPreferencesKey(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_URL)) ?: ""
        } else {
            tileSource.sourceUrl!!
        }
    }

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
    produceState<LatLng?>(initialValue = null) {
        value =
            if (
                context.checkMissingPermissions(Manifest.permission.ACCESS_FINE_LOCATION).isEmpty()
            ) {
                @SuppressLint("MissingPermission")
                locationSource.getLocations(1.seconds, usePassiveProvider = false).first().latLng
            } else {
                // If we don't have location permission, just show any location to avoid crashing
                LatLng(0.0, 0.0)
            }
    }

@Composable
fun AreaPickerMap(
    settingsStore: DataStore<Preferences> = koinInject(PREFERENCES),
    httpClientProvider: Deferred<Call.Factory> = koinInject(),
    onCircleUpdated: (Pair<LatLng, Double>) -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val density = LocalDensity.current

    val mapStyleUrl by settingsStore.mapStyleUrl().collectAsState(initial = null)

    val httpClient = produceState<Call.Factory?>(null) { value = httpClientProvider.await() }

    val currentLocation = getCurrentLocation()

    if (mapStyleUrl == null || httpClient.value == null || currentLocation.value == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(R.string.waiting_for_location))
        }
    } else {
        val mapCenter = remember { mutableStateOf<LatLng?>(null) }

        val circleManager = remember { mutableStateOf<CircleManager?>(null) }
        val circle = remember { mutableStateOf<Circle?>(null) }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapLibre.getInstance(context)
                HttpRequestUtil.setOkHttpClient(httpClient.value)

                val mapView = LifecycleAwareMapView(context)
                mapView.localizeLabelNames()

                mapView.getMapAsync { map ->
                    map.addOnCameraMoveListener {
                        mapCenter.value = map.cameraPosition.target!!.asDomainLatLng()
                    }

                    val cameraPos = currentLocation.value!!.asMapLibreLatLng()

                    map.cameraPosition =
                        CameraPosition.Builder().target(cameraPos).zoom(DEFAULT_ZOOM).build()

                    map.setMinZoomPreference(MIN_ZOOM)
                    map.setMaxZoomPreference(MAX_ZOOM)

                    map.setAttributionMargin(density)

                    map.uiSettings.isRotateGesturesEnabled = false

                    val styleBuilder = Style.Builder().fromUri(mapStyleUrl!!)

                    map.setStyle(styleBuilder) { style ->
                        circleManager.value = CircleManager(mapView, map, style)

                        val radius =
                            density.run {
                                (CIRCLE_SCALE_FACTOR * minOf(map.width, map.height)).toDp() / 2
                            }

                        circle.value =
                            circleManager.value!!.create(
                                createCircle(map.cameraPosition.target!!, radius)
                            )
                    }
                }

                mapView
            },
            update = { mapView ->
                mapView.lifecycle = lifecycle

                val mapCenter = mapCenter.value
                val circle = circle.value

                val circleManager = circleManager.value

                mapView.getMapAsync { map ->
                    if (circle != null && mapCenter != null && circleManager != null) {
                        circle.latLng = mapCenter.asMapLibreLatLng()

                        circleManager.update(circle)

                        onCircleUpdated.invoke(
                            mapCenter to
                                circle.circleRadius *
                                    map.projection.getMetersPerPixelAtLatitude(mapCenter.latitude)
                        )
                    }

                    mapStyleUrl?.let { map.updateMapStyleIfNeeded(it) }
                }
            },
            onRelease = { view -> view.lifecycle = null },
        )
    }
}

private const val CIRCLE_FILL_OPACITY = 0.2f
private const val CIRCLE_STROKE_OPACITY = 0.9f
private const val CIRCLE_STROKE_WIDTH = 2f

private fun createCircle(center: org.maplibre.android.geometry.LatLng, radius: Dp): CircleOptions {
    return CircleOptions()
        .withLatLng(center)
        .withCircleRadius(radius.value)
        .withCircleColor(ColorUtils.colorToRgbaString(Color.CYAN))
        .withCircleOpacity(CIRCLE_FILL_OPACITY)
        .withCircleStrokeColor(ColorUtils.colorToRgbaString(Color.CYAN))
        .withCircleStrokeOpacity(CIRCLE_STROKE_OPACITY)
        .withCircleStrokeWidth(CIRCLE_STROKE_WIDTH)
}
