package xyz.malkki.neostumbler.ui.composables.shared

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Deferred
import okhttp3.Call
import org.koin.compose.koinInject
import org.maplibre.android.MapLibre
import org.maplibre.android.attribution.Attribution
import org.maplibre.android.attribution.AttributionParser
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.module.http.HttpRequestUtil
import org.maplibre.android.style.sources.Source
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getEnumFlow
import xyz.malkki.neostumbler.data.settings.getStringFlow
import xyz.malkki.neostumbler.ui.map.LifecycleAwareMapView
import xyz.malkki.neostumbler.ui.map.MapTileSource

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

    var attributions by remember { mutableStateOf<List<Attribution>>(emptyList()) }

    var attributionDialogOpen by rememberSaveable { mutableStateOf(false) }

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

    if (attributionDialogOpen) {
        AttributionDialog(
            attributions = attributions,
            onDialogClose = { attributionDialogOpen = false },
        )
    }

    mapInstance?.let { updateMap(it) }

    Box(modifier = modifier) {
        if (httpClient != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapLibre.getInstance(context)
                    HttpRequestUtil.setOkHttpClient(httpClient)

                    val mapView = LifecycleAwareMapView(context)
                    mapView.context.registerComponentCallbacks(mapView.componentCallback)

                    mapView.localizeLabelNames()

                    mapView.addOnDidFinishLoadingMapListener {
                        mapView.getMapAsync { map ->
                            map.getStyle { style ->
                                val attrs =
                                    style.sources.flatMap { source ->
                                        source.getAttribution(mapView.context)
                                    }

                                attributions = attrs
                            }
                        }
                    }

                    mapView.getMapAsync { map ->
                        mapTileSourceUrl.let { map.updateMapStyleIfNeeded(it) }

                        map.uiSettings.isAttributionEnabled = false
                        map.uiSettings.isLogoEnabled = false

                        currentOnInit(map, mapView)

                        mapInstance = map
                    }

                    mapView
                },
                update = { mapView ->
                    mapView.lifecycle = lifecycle

                    mapView.getMapAsync { map ->
                        mapTileSourceUrl.let { map.updateMapStyleIfNeeded(it) }

                        mapInstance = map
                    }
                },
                onRelease = { view ->
                    view.context.unregisterComponentCallbacks(view.componentCallback)

                    view.lifecycle = null
                },
            )
        }

        IconButton(
            modifier =
                Modifier.align(Alignment.BottomStart).windowInsetsPadding(WindowInsets.systemBars),
            onClick = { attributionDialogOpen = true },
            enabled = attributions.isNotEmpty(),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.info_24px),
                contentDescription = stringResource(id = R.string.map_data_sources),
                modifier = Modifier.requiredSize(18.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

private fun Source.getAttribution(context: Context): Collection<Attribution> {
    return AttributionParser.Options(context)
        .withMapboxAttribution(false)
        .withImproveMap(false)
        .withCopyrightSign(true)
        .withAttributionData(this.attribution)
        .build()
        .attributions
}

private fun MapLibreMap.updateMapStyleIfNeeded(styleUrl: String) {
    if (style == null || style!!.uri != styleUrl) {
        setStyle(Style.Builder().fromUri(styleUrl))
    }
}
