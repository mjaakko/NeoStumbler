package xyz.malkki.neostumbler.ui.screens

import android.location.Geocoder as AndroidGeocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import java.text.DecimalFormat
import org.koin.androidx.compose.koinViewModel
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.db.entities.ReportWithStats
import xyz.malkki.neostumbler.extensions.defaultLocale
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.scanner.ScannerService
import xyz.malkki.neostumbler.ui.composables.MLSWarningDialog
import xyz.malkki.neostumbler.ui.composables.ReportUploadButton
import xyz.malkki.neostumbler.ui.composables.reports.ForegroundScanningButton
import xyz.malkki.neostumbler.ui.composables.reports.GpsStatus
import xyz.malkki.neostumbler.ui.composables.reports.details.ReportDetailsDialog
import xyz.malkki.neostumbler.ui.composables.shared.CenteredCircularProgressIndicator
import xyz.malkki.neostumbler.ui.composables.shared.ConfirmationDialog
import xyz.malkki.neostumbler.ui.composables.shared.Shimmer
import xyz.malkki.neostumbler.ui.composables.shared.formattedDate
import xyz.malkki.neostumbler.ui.composables.shared.getAddress
import xyz.malkki.neostumbler.ui.viewmodel.ReportsViewModel
import xyz.malkki.neostumbler.utils.geocoder.CachingGeocoder
import xyz.malkki.neostumbler.utils.geocoder.Geocoder
import xyz.malkki.neostumbler.utils.geocoder.PlatformGeocoder

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = koinViewModel()) {
    val density = LocalDensity.current

    var cardHeight by remember { mutableStateOf(0.dp) }

    MLSWarningDialog()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportStats(
                modifier = Modifier.padding(horizontal = 16.dp),
                reportsViewModel = viewModel,
            )

            Reports(
                modifier = Modifier.padding(horizontal = 16.dp).weight(1.0f),
                listBottomPadding = cardHeight,
                reportsViewModel = viewModel,
            )
        }

        ScanningControllerCard(
            modifier =
                Modifier.align(Alignment.BottomCenter).padding(8.dp).onPlaced {
                    cardHeight = density.run { it.size.height.toDp() }
                }
        )
    }
}

@Composable
private fun ScanningControllerCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val scanningActive by ScannerService.serviceRunning.collectAsStateWithLifecycle()
    val reportsCreated by ScannerService.reportsCreated.collectAsStateWithLifecycle()

    ElevatedCard(modifier = modifier.wrapContentHeight().sizeIn(maxWidth = 400.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ForegroundScanningButton()

            Column(modifier = Modifier.align(Alignment.CenterVertically).weight(1.0f)) {
                Text(
                    text =
                        if (scanningActive) {
                            stringResource(R.string.scanning_status_active)
                        } else {
                            stringResource(R.string.scanning_status_not_active)
                        },
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    text =
                        context.getQuantityString(
                            R.plurals.reports_created,
                            reportsCreated,
                            reportsCreated,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            GpsStatus()
        }
    }
}

@Composable
private fun ReportStats(modifier: Modifier = Modifier, reportsViewModel: ReportsViewModel) {
    val reportsTotal = reportsViewModel.reportsTotal.collectAsStateWithLifecycle(null)
    val reportsNotUploaded = reportsViewModel.reportsNotUploaded.collectAsStateWithLifecycle(null)
    val reportsLastUploaded = reportsViewModel.lastUpload.collectAsStateWithLifecycle(null)

    val lastUploadedText =
        reportsLastUploaded.value?.let { formattedDate(it) }
            ?: stringResource(R.string.reports_last_uploaded_never)

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = stringResource(R.string.reports_total, reportsTotal.value ?: 0),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.reports_not_uploaded, reportsNotUploaded.value ?: 0),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.reports_last_uploaded, lastUploadedText),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        ReportUploadButton(reportsViewModel = reportsViewModel)
    }
}

@Composable
private fun Reports(
    modifier: Modifier = Modifier,
    reportsViewModel: ReportsViewModel,
    listBottomPadding: Dp,
) {
    val reports = reportsViewModel.reports.collectAsLazyPagingItems()

    val listState = rememberLazyListState()

    val listAtTop by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    var listWasAtTop by remember { mutableStateOf(listAtTop) }

    val context = LocalContext.current
    val geocoder = remember {
        CachingGeocoder(PlatformGeocoder(AndroidGeocoder(context, context.defaultLocale), 1))
    }

    val reportToShow = rememberSaveable { mutableStateOf<Long?>(null) }

    val reportToDelete = rememberSaveable { mutableStateOf<Long?>(null) }

    if (reportToShow.value != null) {
        ReportDetailsDialog(
            reportId = reportToShow.value!!,
            onDismiss = { reportToShow.value = null },
        )
    }

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
            listState.animateScrollToItem(0)
        }
    }

    SideEffect { listWasAtTop = listAtTop }

    Column(modifier = modifier) {
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
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = listBottomPadding),
            ) {
                items(reports.itemCount, key = reports.itemKey { it.reportId }) { index ->
                    val report = reports[index]

                    Column(modifier = Modifier.wrapContentSize().animateItem()) {
                        if (report != null) {
                            Report(
                                report = report,
                                geocoder = geocoder,
                                onShowReportDetails = { reportId -> reportToShow.value = reportId },
                                onDeleteReport = { reportId -> reportToDelete.value = reportId },
                            )
                        } else {
                            ReportPlaceholder()
                        }

                        if (index < reports.itemCount - 1) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportPlaceholder(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val height = with(density) { 14.sp.toDp() }

    Column(modifier = modifier.wrapContentHeight().padding(vertical = 8.dp)) {
        Row {
            Shimmer(
                modifier =
                    Modifier.height(height)
                        .width(120.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.weight(1.0f))

            Shimmer(
                modifier =
                    Modifier.height(height)
                        .width(60.dp)
                        .background(Color.LightGray, shape = RoundedCornerShape(2.dp))
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Shimmer(
            modifier =
                Modifier.height(height)
                    .width(160.dp)
                    .background(Color.LightGray, shape = RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun Report(
    modifier: Modifier = Modifier,
    report: ReportWithStats,
    geocoder: Geocoder,
    onShowReportDetails: (Long) -> Unit,
    onDeleteReport: (Long) -> Unit,
) {
    val address = getAddress(report.latitude, report.longitude, geocoder = geocoder)

    Column(
        modifier =
            modifier
                .combinedClickable(
                    enabled = true,
                    onClick = { onShowReportDetails(report.reportId) },
                    onLongClickLabel = stringResource(id = R.string.delete_report),
                    onLongClick = { onDeleteReport(report.reportId) },
                )
                .padding(vertical = 8.dp)
                .wrapContentHeight()
    ) {
        Row(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Text(
                modifier = Modifier.wrapContentSize(),
                text = formattedDate(report.timestamp),
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.weight(1.0f))

            EmitterCount(
                icon = Icons.Default.Wifi,
                iconDescription = stringResource(R.string.wifi_icon_description),
                count = report.wifiAccessPointCount,
            )

            Spacer(modifier = Modifier.width(2.dp))

            EmitterCount(
                icon = Icons.Default.CellTower,
                iconDescription = stringResource(R.string.cell_tower_icon_description),
                count = report.cellTowerCount,
            )

            Spacer(modifier = Modifier.width(2.dp))

            EmitterCount(
                icon = Icons.Default.Bluetooth,
                iconDescription = stringResource(R.string.bluetooth_icon_description),
                count = report.bluetoothBeaconCount,
            )
        }

        Text(text = address.value ?: "", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmitterCount(icon: ImageVector, iconDescription: String, count: Int) {
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
