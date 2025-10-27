@file:OptIn(ExperimentalLayoutApi::class)

package xyz.malkki.neostumbler.ui.composables.reports.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DecimalFormat
import org.koin.compose.koinInject
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.report.Report
import xyz.malkki.neostumbler.core.report.ReportEmitter
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.extensions.roundToString
import xyz.malkki.neostumbler.geography.LatLng
import xyz.malkki.neostumbler.ui.composables.shared.formattedDate

private const val WIFIS = 0
private const val CELLS = 1
private const val BLUETOOTHS = 2

@Composable
fun ReportDetailsDialog(reportId: Long, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.sizeIn(maxWidth = 400.dp).fillMaxWidth().wrapContentHeight(),
            shape = AlertDialogDefaults.shape,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(all = 24.dp)) {
                ReportDetails(reportId = reportId)

                TextButton(modifier = Modifier.align(Alignment.End), onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        }
    }
}

// 5 digits ~= 1m precision at the Equator
private const val COORDINATES_DIGITS = 5

@Composable
private fun Coordinates(latLng: LatLng) {
    Text(
        text =
            buildString {
                append(latLng.latitude.roundToString(COORDINATES_DIGITS))
                append(", ")
                append(latLng.longitude.roundToString(COORDINATES_DIGITS))
            },
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun ReportDetails(reportId: Long, reportProvider: ReportProvider = koinInject()) {
    val report = reportProvider.getReport(reportId).collectAsStateWithLifecycle(initialValue = null)

    if (report.value == null) {
        Box(
            modifier = Modifier.height(400.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else {
        Text(
            text = formattedDate(report.value!!.timestamp),
            style = MaterialTheme.typography.titleLarge,
        )

        ReportMap(modifier = Modifier.padding(top = 8.dp), report = report.value!!)

        Spacer(modifier = Modifier.height(8.dp))

        Coordinates(latLng = report.value!!.position.position.latLng)

        val decimalFormat = DecimalFormat("#.#")

        Text(
            text =
                stringResource(
                    R.string.speed_metres_per_second,
                    decimalFormat.format(report.value!!.position.position.speed ?: 0.0),
                ),
            style = MaterialTheme.typography.bodySmall,
        )

        Text(
            text =
                stringResource(
                    R.string.altitude_metres,
                    decimalFormat.format(report.value!!.position.position.altitude ?: 0.0),
                ),
            style = MaterialTheme.typography.bodySmall,
        )

        ReportDataLists(report = report.value!!)
    }
}

@Composable
private fun ReportDataLists(report: Report) {
    val selectedTabIndex = rememberSaveable { mutableIntStateOf(WIFIS) }

    PrimaryTabRow(selectedTabIndex = selectedTabIndex.intValue) {
        arrayOf(WIFIS, CELLS, BLUETOOTHS).forEach { index ->
            Tab(
                selected = selectedTabIndex.intValue == index,
                onClick = { selectedTabIndex.intValue = index },
                text = {
                    Text(
                        text =
                            when (index) {
                                WIFIS -> stringResource(R.string.wifis)
                                CELLS -> stringResource(R.string.cells)
                                BLUETOOTHS -> stringResource(R.string.beacons)
                                else -> ""
                            },
                        style =
                            LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                lineBreak = LineBreak.Heading,
                            ),
                    )
                },
            )
        }
    }

    when (selectedTabIndex.intValue) {
        WIFIS -> {
            if (report.wifiAccessPoints.isEmpty()) {
                NoData()
            } else {
                ReportWifisList(report.wifiAccessPoints)
            }
        }

        CELLS -> {
            if (report.cellTowers.isEmpty()) {
                NoData()
            } else {
                ReportCellsList(report.cellTowers)
            }
        }

        BLUETOOTHS -> {
            if (report.bluetoothBeacons.isEmpty()) {
                NoData()
            } else {
                ReportBluetoothBeaconsList(report.bluetoothBeacons)
            }
        }
    }
}

private val listSize = Modifier.height(160.dp).fillMaxWidth().padding(top = 8.dp)

@Composable
private fun NoData() {
    Box(modifier = listSize, contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.no_data))
    }
}

@Composable
private fun ReportWifisList(wifiAccessPoints: List<ReportEmitter<WifiAccessPoint, MacAddress>>) {
    val sortedWifiAccessPoints =
        remember(wifiAccessPoints) {
            wifiAccessPoints.sortedByDescending { it.emitter.signalStrength }
        }

    LazyColumn(modifier = listSize, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items = sortedWifiAccessPoints, key = { it.id }) { wifiAccessPoint ->
            Column(modifier = Modifier.wrapContentHeight().fillMaxWidth()) {
                Text(
                    text = wifiAccessPoint.emitter.ssid ?: "",
                    style = MaterialTheme.typography.titleSmall,
                )

                Text(
                    text = wifiAccessPoint.emitter.macAddress.value,
                    style = MaterialTheme.typography.bodySmall,
                )

                wifiAccessPoint.emitter.radioType?.to802String()?.let { radioType ->
                    Text(
                        text = stringResource(R.string.radio_type, radioType),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                wifiAccessPoint.emitter.signalStrength?.let { signalStrength ->
                    Text(
                        text = stringResource(R.string.signal_strength_dbm, signalStrength),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportCellsList(cellTowers: List<ReportEmitter<CellTower, String>>) {
    val sortedCellTowers =
        remember(cellTowers) {
            cellTowers
                .sortedByDescending { it.emitter.signalStrength }
                .sortedBy {
                    if (it.emitter.cellId == null) {
                        0
                    } else {
                        -1
                    }
                }
        }

    LazyColumn(modifier = listSize, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items = sortedCellTowers, key = { it.id }) { cellTower ->
            Column(modifier = Modifier.wrapContentHeight().fillMaxWidth()) {
                Text(
                    text =
                        cellTower.emitter.cellId?.toString()
                            ?: stringResource(R.string.unknown_cell_id),
                    style = MaterialTheme.typography.titleSmall,
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    cellTower.emitter.mobileCountryCode?.let { mcc ->
                        Text(
                            text = stringResource(R.string.mcc, mcc),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    cellTower.emitter.mobileNetworkCode?.let { mnc ->
                        Text(
                            text = stringResource(R.string.mnc, mnc),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    cellTower.emitter.locationAreaCode?.let { lac ->
                        Text(
                            text = stringResource(R.string.lac, lac),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Text(
                        text =
                            stringResource(R.string.radio_type, cellTower.emitter.radioType.name),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Text(
                    text =
                        cellTower.emitter.signalStrength?.let {
                            stringResource(R.string.signal_strength_dbm, it)
                        } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private val BEACON_TYPE_FORMATTER = HexFormat { number { removeLeadingZeros = true } }

private const val BEACON_TYPE_CODE_IBEACON = 0x0215
private const val BEACON_TYPE_CODE_ALTBEACON = 0xbeac

@Composable
private fun beaconTypeName(beaconType: Int): String {
    return when (beaconType) {
        BEACON_TYPE_CODE_IBEACON -> stringResource(R.string.beacon_type_ibeacon)
        BEACON_TYPE_CODE_ALTBEACON -> stringResource(R.string.beacon_type_altbeacon)
        else -> beaconType.toHexString(BEACON_TYPE_FORMATTER)
    }
}

@Composable
private fun ReportBluetoothBeaconsList(
    bluetoothBeacons: List<ReportEmitter<BluetoothBeacon, MacAddress>>
) {
    val sortedBluetoothBeaconSource =
        remember(bluetoothBeacons) {
            bluetoothBeacons.sortedByDescending { it.emitter.signalStrength }
        }

    LazyColumn(modifier = listSize, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items = sortedBluetoothBeaconSource, key = { it.id }) { bluetoothBeacon ->
            Column(modifier = Modifier.wrapContentHeight().fillMaxWidth()) {
                Text(
                    text = bluetoothBeacon.emitter.macAddress.value,
                    style = MaterialTheme.typography.titleSmall,
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    bluetoothBeacon.emitter.beaconType?.let { beaconType ->
                        Text(
                            text = stringResource(R.string.beacon_type, beaconTypeName(beaconType)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    listOf(
                            bluetoothBeacon.emitter.id1,
                            bluetoothBeacon.emitter.id2,
                            bluetoothBeacon.emitter.id3,
                        )
                        .forEachIndexed { index, id ->
                            if (id != null) {
                                Text(
                                    text = stringResource(R.string.beacon_id, index + 1, id),
                                    maxLines = 1,
                                    overflow = TextOverflow.MiddleEllipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                }

                Text(
                    text =
                        bluetoothBeacon.emitter.signalStrength.let {
                            stringResource(R.string.signal_strength_dbm, it)
                        },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
