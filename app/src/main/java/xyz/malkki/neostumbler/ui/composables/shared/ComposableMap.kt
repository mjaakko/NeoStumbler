package xyz.malkki.neostumbler.ui.composables.shared

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Deferred
import okhttp3.Call
import org.koin.compose.koinInject
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getEnumFlow
import xyz.malkki.neostumbler.data.settings.getStringFlow
import xyz.malkki.neostumbler.ui.map.LifecycleAwareMapView
import xyz.malkki.neostumbler.ui.map.MapTileSource
import xyz.malkki.neostumbler.ui.map.updateMapStyleIfNeeded

@Composable
fun ComposableMap(
    modifier: Modifier = Modifier,
    onInit: @DisallowComposableCalls (MapLibreMap, MapView) -> Unit,
    updateMap: @Composable (MapLibreMap) -> Unit,
    httpClientProvider: Deferred<Call.Factory> = koinInject(),
    settings: Settings = koinInject(),
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val currentOnInit by rememberUpdatedState(onInit)

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null, neverEqualPolicy()) }

    val httpClient by
        produceState<Call.Factory?>(null, httpClientProvider) { value = httpClientProvider.await() }

    val mapTileSource by
        settings
            .getEnumFlow(PreferenceKeys.MAP_TILE_SOURCE, MapTileSource.DEFAULT)
            .collectAsStateWithLifecycle(initialValue = MapTileSource.DEFAULT)

    val customMapStyleUrl by
        settings
            .getStringFlow(PreferenceKeys.MAP_TILE_SOURCE_CUSTOM_URL, "")
            .collectAsStateWithLifecycle(initialValue = "")

    val mapTileSourceUrl =
        if (mapTileSource == MapTileSource.CUSTOM) {
            customMapStyleUrl
        } else if (isSystemInDarkTheme() && mapTileSource.sourceUrlDark != null) {
            mapTileSource.sourceUrlDark!!
        } else {
            mapTileSource.sourceUrl!!
        }

    mapInstance?.let { updateMap(it) }

    if (httpClient != null) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                MapLibre.getInstance(context)
                HttpRequestUtil.setOkHttpClient(httpClient)

                val mapView = LifecycleAwareMapView(context)
                mapView.context.registerComponentCallbacks(mapView.componentCallback)

                mapView.localizeLabelNames()

                mapView.getMapAsync { map ->
                    map.setStyle(Style.Builder().fromUri(mapTileSourceUrl))

                    currentOnInit(map, mapView)

                    mapInstance = map
                }

                mapView
            },
            update = { mapView ->
                mapView.lifecycle = lifecycle

                mapView.getMapAsync { map ->
                    mapTileSourceUrl.let { map.updateMapStyleIfNeeded(mapTileSourceUrl) }

                    mapInstance = map
                }
            },
            onRelease = { view ->
                view.context.unregisterComponentCallbacks(view.componentCallback)

                view.lifecycle = null
            },
        )
    }
}
