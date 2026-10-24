package xyz.malkki.neostumbler.ui.composables.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.constants.PreferenceKeys
import xyz.malkki.neostumbler.ui.composables.ToggleWithAction
import xyz.malkki.neostumbler.ui.composables.settings.SettingsToggle
import xyz.malkki.neostumbler.ui.composables.settings.UrlField
import xyz.malkki.neostumbler.ui.composables.shared.Dialog
import xyz.malkki.neostumbler.ui.composables.shared.FilePicker
import xyz.malkki.neostumbler.ui.map.CustomMapTileSource
import xyz.malkki.neostumbler.ui.map.MapSettingsManager
import xyz.malkki.neostumbler.ui.map.MapTileSource
import xyz.malkki.neostumbler.ui.map.OverrideMapTheme
import xyz.malkki.neostumbler.ui.map.PredefinedMapTileSource

@Composable
fun MapSettingsButton(modifier: Modifier) {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    if (dialogOpen) {
        MapSettingsDialog(onCloseDialog = { dialogOpen = false })
    }

    FilledTonalIconButton(
        modifier = modifier,
        onClick = { dialogOpen = true },
        colors = IconButtonDefaults.filledTonalIconButtonColors(),
    ) {
        Icon(
            modifier = Modifier.requiredSize(18.dp),
            painter = painterResource(id = R.drawable.layers_24px),
            contentDescription = stringResource(id = R.string.map_tile_source),
        )
    }
}

@Composable
private fun MapSettingsDialog(
    mapSettingsManager: MapSettingsManager = koinInject(),
    onCloseDialog: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val selectedMapTileSource by
        mapSettingsManager.selectedMapStyle.collectAsStateWithLifecycle(initialValue = null)

    Dialog(
        title = stringResource(id = R.string.map_tile_source),
        onDismissRequest = onCloseDialog,
    ) {
        Column {
            Column(
                modifier = Modifier.selectableGroup().padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapTileSource.options.forEach { mapTileSource ->
                    Row(
                        Modifier.fillMaxWidth()
                            .wrapContentHeight()
                            .defaultMinSize(minHeight = 36.dp)
                            .selectable(
                                selected = mapTileSource == selectedMapTileSource,
                                onClick = {
                                    coroutineScope.launch {
                                        mapSettingsManager.setMapTileSource(mapTileSource)
                                    }
                                },
                                role = Role.RadioButton,
                            )
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            modifier =
                                Modifier.align(Alignment.CenterVertically).padding(top = 4.dp),
                            selected = mapTileSource == selectedMapTileSource,
                            onClick = null,
                        )

                        Text(
                            modifier =
                                Modifier.align(Alignment.CenterVertically).padding(start = 16.dp),
                            text =
                                when (mapTileSource) {
                                    is CustomMapTileSource ->
                                        stringResource(mapTileSource.titleResourceId)
                                    is PredefinedMapTileSource -> mapTileSource.title
                                },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    if (mapTileSource == selectedMapTileSource) {
                        when (selectedMapTileSource) {
                            CustomMapTileSource.CUSTOM_URL -> {
                                CustomMapStyleUrl(mapSettingsManager)
                            }

                            CustomMapTileSource.CUSTOM_FILE -> {
                                CustomMapStyleFile(mapSettingsManager)
                            }

                            else -> {}
                        }
                    }
                }
            }

            HorizontalDivider()

            Column(modifier = Modifier.padding(all = 8.dp)) {
                val mapThemeOverride by
                    mapSettingsManager.mapThemeOverride.collectAsStateWithLifecycle(
                        initialValue = OverrideMapTheme.SYSTEM
                    )

                ToggleWithAction(
                    title = stringResource(R.string.map_always_in_light_mode),
                    checked = mapThemeOverride == OverrideMapTheme.LIGHT,
                    action = { checked ->
                        mapSettingsManager.setMapThemeOverride(
                            if (checked) {
                                OverrideMapTheme.LIGHT
                            } else {
                                OverrideMapTheme.SYSTEM
                            }
                        )
                    },
                )

                SettingsToggle(
                    title = stringResource(R.string.map_show_coverage_layer),
                    preferenceKey = PreferenceKeys.COVERAGE_LAYER_ENABLED,
                    default = true,
                )
            }
        }
    }
}

@Composable
private fun CustomMapStyleUrl(mapSettingsManager: MapSettingsManager) {
    val mapStyleUrl = rememberTextFieldState()

    LaunchedEffect(Unit) {
        mapSettingsManager.selectedMapStyle
            .flatMapLatest {
                if (it == CustomMapTileSource.CUSTOM_URL) {
                    mapSettingsManager.selectedMapStyleUrl
                } else {
                    emptyFlow()
                }
            }
            .collectLatest {
                mapStyleUrl.setTextAndPlaceCursorAtEnd(it.styleUrl)
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { mapStyleUrl.text }
            .drop(1)
            .collectLatest { mapStyleUrl ->
                mapSettingsManager.setCustomUrlMapTileSource(mapStyleUrl.toString())
            }
    }

    UrlField(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        label = stringResource(R.string.map_tile_source_custom_url_description),
        state = mapStyleUrl,
    )
}

@Composable
private fun CustomMapStyleFile(mapSettingsManager: MapSettingsManager) {
    val coroutineScope = rememberCoroutineScope()

    val mapStyleFile by
        mapSettingsManager.mapStyleFile.collectAsStateWithLifecycle(initialValue = null)

    FilePicker(
        modifier = Modifier.padding(horizontal = 8.dp),
        directory = LocalContext.current.dataDir.resolve("map_settings"),
        defaultFileName = "style.json",
        selectedFile = mapStyleFile?.toFile(),
        onFileSelected = { file ->
            coroutineScope.launch { mapSettingsManager.setCustomFileMapTileSource(file.toPath()) }
        },
        onFileDeleted = {
            coroutineScope.launch { mapSettingsManager.setCustomFileMapTileSource(null) }
        },
    )
}
