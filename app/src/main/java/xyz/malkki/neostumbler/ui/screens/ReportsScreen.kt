package xyz.malkki.neostumbler.ui.screens

import android.location.Geocoder as AndroidGeocoder
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import java.text.DecimalFormat
import java.util.Date
import org.koin.androidx.compose.koinViewModel
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.db.entities.ReportWithStats
import xyz.malkki.neostumbler.extensions.defaultLocale
import xyz.malkki.neostumbler.ui.composables.MLSWarningDialog
import xyz.malkki.neostumbler.ui.composables.ReportUploadButton
import xyz.malkki.neostumbler.ui.composables.reports.ForegroundScanningButton
import xyz.malkki.neostumbler.ui.composables.shared.CenteredCircularProgressIndicator
import xyz.malkki.neostumbler.ui.composables.shared.ConfirmationDialog
import xyz.malkki.neostumbler.ui.composables.shared.Link
import xyz.malkki.neostumbler.ui.composables.shared.Shimmer
import xyz.malkki.neostumbler.ui.composables.shared.getAddress
import xyz.malkki.neostumbler.ui.viewmodel.ReportsViewModel
import xyz.malkki.neostumbler.utils.geocoder.CachingGeocoder
import xyz.malkki.neostumbler.utils.geocoder.Geocoder
import xyz.malkki.neostumbler.utils.geocoder.PlatformGeocoder
import xyz.malkki.neostumbler.utils.showMapWithMarkerIntent

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = koinViewModel()) {
    MLSWarningDialog()

    Column(modifier = Modifier.padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForegroundScanningButton()

            Spacer(modifier = Modifier.weight(1.0f))

            ReportUploadButton(viewModel)
        }
        ReportStats(viewModel)
        Reports(viewModel)
    }
}

@Composable
private fun ReportStats(reportsViewModel: ReportsViewModel) {
    val context = LocalContext.current

    val reportsTotal = reportsViewModel.reportsTotal.collectAsStateWithLifecycle(null)
    val reportsNotUploaded = reportsViewModel.reportsNotUploaded.collectAsStateWithLifecycle(null)
    val reportsLastUploaded = reportsViewModel.lastUpload.collectAsStateWithLifecycle(null)

    val lastUploadedText =
        reportsLastUploaded.value?.let {
            val millis = it.toEpochMilli()

            DateFormat.getMediumDateFormat(context).format(millis) +
                " " +
                DateFormat.getTimeFormat(context).format(millis)
        } ?: stringResource(R.string.reports_last_uploaded_never)

    Column(modifier = Modifier.wrapContentHeight()) {
        Text(text = stringResource(R.string.reports_total, reportsTotal.value ?: 0))
        Text(text = stringResource(R.string.reports_not_uploaded, reportsNotUploaded.value ?: 0))
        Text(text = stringResource(R.string.reports_last_uploaded, lastUploadedText))
    }
}

@Composable
private fun Reports(reportsViewModel: ReportsViewModel) {
    val reports = reportsViewModel.reports.collectAsLazyPagingItems()

    val listState = rememberLazyListState()

    val listAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    var listWasAtTop by remember { mutableStateOf(listAtTop) }

    val context = LocalContext.current
    val geocoder = remember {
        CachingGeocoder(PlatformGeocoder(AndroidGeocoder(context, context.defaultLocale), 1))
    }

    val reportToDelete = rememberSaveable { mutableStateOf<Long?>(null) }

    if (reportToDelete.value != null) {
        ConfirmationDialog(
            title = stringResource(id = R.string.delete_report),
            description = stringResource(id = R.string.delete_report_confirmation),
            positiveButtonText = stringResource(id = R.string.yes),
            negativeButtonText = stringResource(id = R.string.no),
            onPositiveAction = {
                reportsViewModel.deleteReport(reportToDelete.value!!)

                reportToDelete.value = null
            },
            onNegativeAction = { reportToDelete.value = null },
        )
    }

    LaunchedEffect(reports.itemCount) {
        // Scroll list to the top if it was at the top before
        if (listWasAtTop) {
            listState.scrollToItem(0)
        }
    }

    SideEffect { listWasAtTop = listAtTop }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = stringResource(R.string.reports), style = MaterialTheme.typography.titleMedium)

        if (reports.itemCount == 0) {
            if (reports.loadState.isIdle) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.reports_empty))
                }
            } else {
                CenteredCircularProgressIndicator()
            }
        } else {
            LazyColumn(state = listState) {
                items(reports.itemCount, key = reports.itemKey { it.reportId }) { index ->
                    val report = reports.get(index)

                    if (report != null) {
                        Report(
                            report = report,
                            geocoder = geocoder,
                            onDeleteReport = { reportId -> reportToDelete.value = reportId },
                        )
                    } else {
                        ReportPlaceholder()
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportPlaceholder() {
    val density = LocalDensity.current
    val height = with(density) { 14.sp.toDp() }

    Column(modifier = Modifier.wrapContentHeight().padding(vertical = 4.dp)) {
        Shimmer(
            modifier =
                Modifier.height(height)
                    .fillMaxWidth()
                    .background(Color.LightGray, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.height(2.dp))
        Shimmer(
            modifier =
                Modifier.height(height)
                    .fillMaxWidth()
                    .background(Color.LightGray, shape = RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun Report(report: ReportWithStats, geocoder: Geocoder, onDeleteReport: (Long) -> Unit) {
    val context = LocalContext.current

    val address = getAddress(report.latitude, report.longitude, geocoder = geocoder)

    val date = Date.from(report.timestamp)
    val dateStr =
        "${DateFormat.getMediumDateFormat(context).format(date)} ${DateFormat.getTimeFormat(context).format(date)}"

    val intent = showMapWithMarkerIntent(report.latitude, report.longitude)
    val canShowMap = intent.resolveActivity(context.packageManager) != null

    Column(
        modifier =
            Modifier.combinedClickable(
                    enabled = true,
                    onClick = {},
                    onLongClickLabel = stringResource(id = R.string.delete_report),
                    onLongClick = { onDeleteReport(report.reportId) },
                )
                .padding(vertical = 4.dp)
                .wrapContentHeight()
    ) {
        Row(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Text(
                modifier = Modifier.wrapContentSize(),
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.weight(1.0f))
            StationCount(
                icon = Icons.Default.Wifi,
                iconDescription = stringResource(R.string.wifi_icon_description),
                count = report.wifiAccessPointCount,
            )
            Spacer(modifier = Modifier.width(2.dp))
            StationCount(
                icon = Icons.Default.CellTower,
                iconDescription = stringResource(R.string.cell_tower_icon_description),
                count = report.cellTowerCount,
            )
            Spacer(modifier = Modifier.width(2.dp))
            StationCount(
                icon = Icons.Default.Bluetooth,
                iconDescription = stringResource(R.string.bluetooth_icon_description),
                count = report.bluetoothBeaconCount,
            )
        }
        if (canShowMap) {
            Link(
                text = address.value,
                onClick = { context.startActivity(intent) },
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Text(text = address.value, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StationCount(icon: ImageVector, iconDescription: String, count: Int) {
    val decimalFormat = remember { DecimalFormat("0") }

    val localDensity = LocalDensity.current
    var textHeightDp by remember { mutableStateOf(0.dp) }

    Row(
        modifier = Modifier.wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            painter = rememberVectorPainter(icon),
            contentDescription = iconDescription,
            modifier = Modifier.requiredSize(textHeightDp),
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            modifier =
                Modifier.wrapContentWidth().fillMaxHeight().onGloballyPositioned {
                    textHeightDp = with(localDensity) { it.size.height.toDp() }
                },
            text = decimalFormat.format(count),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
