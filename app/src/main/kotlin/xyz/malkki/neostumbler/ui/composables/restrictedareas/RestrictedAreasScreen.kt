package xyz.malkki.neostumbler.ui.composables.restrictedareas

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.lang.ref.WeakReference
import java.util.UUID
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Projection
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.data.restrictedarea.RestrictedArea
import xyz.malkki.neostumbler.domain.asDomainLatLng
import xyz.malkki.neostumbler.domain.asMapLibreLatLng
import xyz.malkki.neostumbler.geography.Circle
import xyz.malkki.neostumbler.ui.composables.shared.AreaPickerDialog
import xyz.malkki.neostumbler.ui.composables.shared.ComposableMap
import xyz.malkki.neostumbler.ui.composables.shared.ConfirmationDialog
import xyz.malkki.neostumbler.ui.map.isCloseTo
import xyz.malkki.neostumbler.ui.viewmodel.RestrictedAreasViewModel
import xyz.malkki.neostumbler.utils.maplibre.needsRecreation

private const val MIN_ZOOM = 3.0
private const val MAX_ZOOM = 15.5

private const val RESTRICTED_AREAS_SOURCE_ID = "restricted-areas"

private const val RESTRICTED_AREAS_LAYER_ID = "restricted-areas-layer"

private const val PROP_ID = "id"
private const val PROP_SELECTED = "selected"
private const val PROP_RADIUS_METERS = "radiusMeters"
private const val PROP_METERS_PER_PIXEL = "metersPerPixel"

@Composable
fun RestrictedAreasScreen(viewModel: RestrictedAreasViewModel = koinViewModel()) {
    val showExplanation by viewModel.showExplanation.collectAsStateWithLifecycle()

    if (showExplanation) {
        RestrictedAreasExplanationDialog(onClose = viewModel::closeExplanation)
    }

    val restrictedAreas by viewModel.restrictedAreas.collectAsStateWithLifecycle()
    val selectedRestrictedAreas by viewModel.selectedRestrictedAreas.collectAsStateWithLifecycle()

    var mapProjection by remember { mutableStateOf<WeakReference<Projection>?>(null) }

    val restrictedAreaFeatures: List<Feature> =
        remember(restrictedAreas, selectedRestrictedAreas, mapProjection) {
            restrictedAreas.toFeatures(selectedRestrictedAreas, mapProjection?.get())
        }

    var circleGeoJsonSource by remember {
        mutableStateOf(GeoJsonSource(RESTRICTED_AREAS_SOURCE_ID))
    }

    LaunchedEffect(restrictedAreaFeatures, circleGeoJsonSource) {
        circleGeoJsonSource.setGeoJson(FeatureCollection.fromFeatures(restrictedAreaFeatures))
    }

    val mapViewport by viewModel.mapViewport.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        ComposableMap(
            modifier = Modifier.fillMaxSize(),
            onInit = { map, _ ->
                map.addOnCameraMoveListener {
                    viewModel.setMapViewport(
                        center = map.cameraPosition.target!!.asDomainLatLng(),
                        zoom = map.cameraPosition.zoom,
                    )

                    mapProjection = WeakReference(map.projection)
                }

                map.addOnMapLongClickListener(
                    OnRestrictedAreaLongClick(map) { id ->
                        if (id in selectedRestrictedAreas) {
                            viewModel.deselectRestrictedAreaById(id)
                        } else {
                            viewModel.selectRestrictedAreaById(id)
                        }
                    }
                )

                map.uiSettings.isRotateGesturesEnabled = false

                map.setMinZoomPreference(MIN_ZOOM)
                map.setMaxZoomPreference(MAX_ZOOM)
            },
            onStyleUpdated = { style ->
                if (circleGeoJsonSource.needsRecreation()) {
                    circleGeoJsonSource = GeoJsonSource(RESTRICTED_AREAS_SOURCE_ID)
                }

                if (style.getSource(RESTRICTED_AREAS_SOURCE_ID) == null) {
                    style.addSource(circleGeoJsonSource)

                    style.addLayer(
                        createRestrictedAreasCircleLayer(RESTRICTED_AREAS_SOURCE_ID, Color.Red)
                    )
                }
            },
            updateMap = { map ->
                // Only update the camera position when needed to avoid infinite loop
                if (!map.cameraPosition.target!!.isCloseTo(mapViewport.first.asMapLibreLatLng())) {
                    map.cameraPosition =
                        CameraPosition.Builder()
                            .target(mapViewport.first.asMapLibreLatLng())
                            .zoom(mapViewport.second)
                            .build()
                }
            },
        )

        Box(
            modifier =
                Modifier.fillMaxSize().padding(16.dp).windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            AnimatedContent(
                modifier = Modifier.wrapContentSize().align(Alignment.BottomEnd),
                targetState = selectedRestrictedAreas.isNotEmpty(),
                contentAlignment = Alignment.BottomEnd,
            ) { areasSelected ->
                if (areasSelected) {
                    DeleteRestrictedAreas(
                        modifier = Modifier.size(48.dp),
                        onDelete = viewModel::deleteSelectedRestrictedAreas,
                    )
                } else {
                    AddNewRestrictedAreaButton(
                        modifier = Modifier.size(48.dp),
                        addRestrictedArea = { viewModel.addRestrictedArea(it) },
                    )
                }
            }
        }
    }
}

private class OnRestrictedAreaLongClick(
    private val map: MapLibreMap,
    private val onLongClick: (UUID) -> Unit,
) : MapLibreMap.OnMapLongClickListener {
    override fun onMapLongClick(clickedCoordinates: LatLng): Boolean {
        map.queryRenderedFeatures(
                map.projection.toScreenLocation(clickedCoordinates),
                RESTRICTED_AREAS_LAYER_ID,
            )
            .firstOrNull()
            ?.let { restrictedAreaFeature ->
                restrictedAreaFeature
                    .properties()
                    ?.getAsJsonPrimitive(PROP_ID)
                    ?.asString
                    ?.let { UUID.fromString(it) }
                    ?.let { id -> onLongClick(id) }
            }

        return true
    }
}

@Composable
private fun RestrictedAreasExplanationDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.ok)) } },
        title = { Text(stringResource(R.string.restricted_areas_settings_title)) },
        text = { Text(stringResource(R.string.restricted_areas_explanation)) },
    )
}

@Composable
private fun DeleteRestrictedAreas(modifier: Modifier, onDelete: () -> Unit) {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    if (dialogOpen) {
        ConfirmationDialog(
            title = stringResource(R.string.restricted_areas_delete),
            description = stringResource(R.string.restricted_areas_delete_confirmation),
            positiveButtonText = stringResource(R.string.yes),
            negativeButtonText = stringResource(R.string.no),
            onPositiveAction = onDelete,
            onNegativeAction = { dialogOpen = false },
        )
    }

    FilledTonalIconButton(modifier = modifier, onClick = { dialogOpen = true }) {
        Icon(
            painter = painterResource(R.drawable.delete_forever_24px),
            contentDescription = stringResource(R.string.restricted_areas_delete),
        )
    }
}

@Composable
private fun AddNewRestrictedAreaButton(modifier: Modifier, addRestrictedArea: (Circle) -> Unit) {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    if (dialogOpen) {
        AreaPickerDialog(
            title = stringResource(R.string.restricted_areas_add),
            positiveButtonText = stringResource(R.string.save),
            onAreaSelected = { circle ->
                dialogOpen = false

                circle?.let { addRestrictedArea(it) }
            },
        )
    }

    FilledIconButton(modifier = modifier, onClick = { dialogOpen = true }) {
        Icon(
            painter = painterResource(R.drawable.add_24px),
            contentDescription = stringResource(R.string.restricted_areas_add),
        )
    }
}

private fun Collection<RestrictedArea>.toFeatures(
    selected: Set<UUID>,
    mapProjection: Projection?,
): List<Feature> {
    return map { restrictedArea ->
        Feature.fromGeometry(
                Point.fromLngLat(
                    restrictedArea.circle.center.longitude,
                    restrictedArea.circle.center.latitude,
                )
            )
            .apply {
                addStringProperty(PROP_ID, restrictedArea.id.toString())
                addBooleanProperty(PROP_SELECTED, restrictedArea.id in selected)
                addNumberProperty(PROP_RADIUS_METERS, restrictedArea.circle.radius)

                mapProjection?.let { projection ->
                    addNumberProperty(
                        PROP_METERS_PER_PIXEL,
                        projection.getMetersPerPixelAtLatitude((geometry() as Point).latitude()),
                    )
                }
            }
    }
}

private const val FILL_OPACITY = 0.3f
private const val STROKE_OPACITY = 0.6f

private const val SELECTED_EXTRA_OPACITY = 0.4f

private fun createRestrictedAreasCircleLayer(sourceId: String, color: Color): CircleLayer {
    return CircleLayer(RESTRICTED_AREAS_LAYER_ID, sourceId).apply {
        setProperties(
            PropertyFactory.circleRadius(
                Expression.division(
                    Expression.get(PROP_RADIUS_METERS),
                    Expression.get(PROP_METERS_PER_PIXEL),
                )
            ),
            PropertyFactory.circleColor(Expression.color(color.toArgb())),
            PropertyFactory.circleOpacity(
                Expression.switchCase(
                    Expression.get(PROP_SELECTED),
                    Expression.literal(FILL_OPACITY + SELECTED_EXTRA_OPACITY),
                    Expression.literal(FILL_OPACITY),
                )
            ),
            PropertyFactory.circleStrokeWidth(2f),
            PropertyFactory.circleStrokeOpacity(
                Expression.switchCase(
                    Expression.get(PROP_SELECTED),
                    Expression.literal(STROKE_OPACITY + SELECTED_EXTRA_OPACITY),
                    Expression.literal(STROKE_OPACITY),
                )
            ),
            PropertyFactory.circleStrokeColor(Expression.color(color.toArgb())),
        )
    }
}
