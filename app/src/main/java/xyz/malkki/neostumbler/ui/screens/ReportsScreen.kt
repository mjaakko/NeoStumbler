package xyz.malkki.neostumbler.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.MainActivity
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.db.entities.ReportWithStats
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.defaultLocale
import xyz.malkki.neostumbler.extensions.getActivity
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.scanner.ScannerService
import xyz.malkki.neostumbler.scanner.quicksettings.ScannerTileService
import xyz.malkki.neostumbler.ui.composables.AddQSTileDialog
import xyz.malkki.neostumbler.ui.composables.BatteryOptimizationsDialog
import xyz.malkki.neostumbler.ui.composables.ConfirmationDialog
import xyz.malkki.neostumbler.ui.composables.Link
import xyz.malkki.neostumbler.ui.composables.MLSWarningDialog
import xyz.malkki.neostumbler.ui.composables.PermissionsDialog
import xyz.malkki.neostumbler.ui.composables.ReportUploadButton
import xyz.malkki.neostumbler.ui.composables.Shimmer
import xyz.malkki.neostumbler.ui.composables.getAddress
import xyz.malkki.neostumbler.ui.composables.rememberServiceConnection
import xyz.malkki.neostumbler.ui.viewmodel.ReportsViewModel
import xyz.malkki.neostumbler.utils.OneTimeActionHelper
import xyz.malkki.neostumbler.utils.geocoder.CachingGeocoder
import xyz.malkki.neostumbler.utils.geocoder.Geocoder
import xyz.malkki.neostumbler.utils.geocoder.PlatformGeocoder
import xyz.malkki.neostumbler.utils.showMapWithMarkerIntent
import java.text.DecimalFormat
import java.util.Date
import android.location.Geocoder as AndroidGeocoder

@Composable
fun ReportsScreen() {
    MLSWarningDialog()

    Column(modifier = Modifier.padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForegroundScanningButton()

            Spacer(modifier = Modifier.weight(1.0f))

            ReportUploadButton()
        }
        ReportStats()
        Reports()
    }
}

@Composable
private fun ReportStats(reportsViewModel: ReportsViewModel = viewModel()) {
    val context = LocalContext.current

    val reportsTotal = reportsViewModel.reportsTotal.collectAsStateWithLifecycle(null)
    val reportsNotUploaded = reportsViewModel.reportsNotUploaded.collectAsStateWithLifecycle(null)
    val reportsLastUploaded = reportsViewModel.lastUpload.collectAsStateWithLifecycle(null)

    val lastUploadedText = reportsLastUploaded.value?.let {
        val millis = it.toEpochMilli()

        DateFormat.getMediumDateFormat(context).format(millis) + " " + DateFormat.getTimeFormat(context).format(millis)
    }

    Column(
        modifier = Modifier.wrapContentHeight()
    ) {
        Text(text = stringResource(R.string.reports_total, reportsTotal.value ?: 0))
        Text(text = stringResource(R.string.reports_not_uploaded, reportsNotUploaded.value ?: 0))
        Text(text = stringResource(R.string.reports_last_uploaded, lastUploadedText ?: ""))
    }
}

private val requiredPermissions = mutableListOf<String>()
    .apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.READ_PHONE_STATE)
    }
    .toTypedArray()

@SuppressLint("NewApi")
@Composable
fun ForegroundScanningButton() {
    val context = LocalContext.current
    val intent = context.getActivity()?.intent

    val coroutineScope = rememberCoroutineScope()
    val oneTimeActionHelper = OneTimeActionHelper(context.applicationContext as StumblerApplication)

    val serviceConnection = rememberServiceConnection(getService = ScannerService.ScannerServiceBinder::getService)

    val showBatteryOptimizationsDialog = rememberSaveable {
        mutableStateOf(false)
    }

    val showPermissionDialog = rememberSaveable {
        mutableStateOf(false)
    }

    val showBackgroundLocationPermissionDialog = rememberSaveable {
        mutableStateOf(false)
    }

    val showQuickSettingsDialog = rememberSaveable {
        mutableStateOf(false)
    }

    val missingPermissions = rememberSaveable {
        context.checkMissingPermissions(*requiredPermissions)
    }

    fun startScanning() {
        if (ScannerService.serviceRunning.value) {
            context.startService(ScannerService.stopIntent(context))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                //Prompt user to add the quick settings tile for scanning
                coroutineScope.launch {
                    if (!oneTimeActionHelper.hasActionBeenShown(ScannerTileService.ADD_QS_TILE_ACTION_NAME)) {
                        showQuickSettingsDialog.value = true
                    }
                }
            }
        } else {
            if (Manifest.permission.ACCESS_FINE_LOCATION !in missingPermissions) {
                if (intent?.getBooleanExtra(MainActivity.EXTRA_REQUEST_BACKGROUND_PERMISSION, false) == true
                    && context.checkMissingPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION).isNotEmpty()) {
                    showBackgroundLocationPermissionDialog.value = true
                } else {
                    showBatteryOptimizationsDialog.value = true
                }
            } else {
                showPermissionDialog.value = true
            }
        }
    }

    val onPermissionsGranted: (Map<String, Boolean>) -> Unit = { permissions ->
        showPermissionDialog.value = false

        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            if (intent?.getBooleanExtra(MainActivity.EXTRA_REQUEST_BACKGROUND_PERMISSION, false) == true
                && context.checkMissingPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION).isNotEmpty()) {
                showBackgroundLocationPermissionDialog.value = true
            } else {
                showBatteryOptimizationsDialog.value = true
            }
        } else {
            context.showToast(ContextCompat.getString(context, R.string.permissions_not_granted))
        }
    }

    if (showQuickSettingsDialog.value) {
        AddQSTileDialog(
            componentName = ComponentName(context, ScannerTileService::class.java),
            dialogText = stringResource(id = R.string.add_quick_settings_tile),
            onDialogDismissed = {
                coroutineScope.launch {
                    oneTimeActionHelper.markActionShown(ScannerTileService.ADD_QS_TILE_ACTION_NAME)
                }

                showQuickSettingsDialog.value = false
            }
        )
    }

    if (showPermissionDialog.value) {
        PermissionsDialog(
            missingPermissions = missingPermissions,
            permissionRationales = mutableMapOf<String, String>().apply {
                put(Manifest.permission.ACCESS_FINE_LOCATION, stringResource(id = R.string.permission_rationale_fine_location))
                put(Manifest.permission.READ_PHONE_STATE, stringResource(id = R.string.permission_rationale_read_phone_state))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    put(Manifest.permission.POST_NOTIFICATIONS, stringResource(id = R.string.permission_rationale_post_notifications))
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    put(Manifest.permission.BLUETOOTH_SCAN, stringResource(id = R.string.permission_rationale_bluetooth))
                } else {
                    put(Manifest.permission.BLUETOOTH, stringResource(id = R.string.permission_rationale_bluetooth))
                    put(Manifest.permission.BLUETOOTH_ADMIN, stringResource(id = R.string.permission_rationale_bluetooth))
                }
            },
            onPermissionsGranted = onPermissionsGranted
        )
    }

    if (showBackgroundLocationPermissionDialog.value) {
        PermissionsDialog(
            missingPermissions = listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            permissionRationales = mapOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION to stringResource(id = R.string.permission_rationale_background_location_quick_settings)
            ),
            onPermissionsGranted = {
                showBackgroundLocationPermissionDialog.value = false

                showBatteryOptimizationsDialog.value = true
            })
    }

    if (showBatteryOptimizationsDialog.value) {
        BatteryOptimizationsDialog(onBatteryOptimizationsDisabled = {
            showBatteryOptimizationsDialog.value = false
            context.startForegroundService(ScannerService.startIntent(context))
        })
    }

    LaunchedEffect(intent) {
        if (intent?.getBooleanExtra(MainActivity.EXTRA_START_SCANNING, false) == true) {
            startScanning()
        }
    }

    Button(
        onClick = {
            startScanning()
        }
    ) {
        val isScanning = serviceConnection.value != null

        val stringResId = if (isScanning) {
            R.string.stop_scanning
        } else {
            R.string.start_scanning
        }
        val icon = if (isScanning) {
            Icons.Default.Stop
        } else {
            Icons.Default.PlayArrow
        }

        Icon(
            painter = rememberVectorPainter(icon),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))

        Text(text = stringResource(stringResId))
    }
}

@Composable
private fun Reports(reportsViewModel: ReportsViewModel = viewModel()) {
    val reports = reportsViewModel.reports.collectAsLazyPagingItems()

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
            onNegativeAction = {
                reportToDelete.value = null
            })
    }

    Column(
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.reports),
            style = MaterialTheme.typography.titleMedium,
        )
        LazyColumn {
            items(
                reports.itemCount,
                key = reports.itemKey { it.reportId }
            ) { index ->
                val report = reports.get(index)

                if (report != null) {
                    Report(
                        report = report,
                        geocoder = geocoder,
                        onDeleteReport = { reportId ->
                            reportToDelete.value = reportId
                        })
                } else {
                    ReportPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun ReportPlaceholder() {
    val density = LocalDensity.current
    val height = with(density) { 14.sp.toDp() }

    Column(
        modifier = Modifier
            .wrapContentHeight()
            .padding(vertical = 4.dp)
    ) {
        Shimmer(
            modifier = Modifier
                .height(height)
                .fillMaxWidth()
                .background(Color.LightGray, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.height(2.dp))
        Shimmer(
            modifier = Modifier
                .height(height)
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
    val dateStr = "${DateFormat.getMediumDateFormat(context).format(date)} ${DateFormat.getTimeFormat(context).format(date)}"

    val intent = showMapWithMarkerIntent(report.latitude, report.longitude)
    val canShowMap = intent.resolveActivity(context.packageManager) != null

    Column(
        modifier = Modifier
            .combinedClickable(
                enabled = true,
                onClick = {},
                onLongClickLabel = stringResource(id = R.string.delete_report),
                onLongClick = {
                    onDeleteReport(report.reportId)
                }
            )
            .padding(vertical = 4.dp)
            .wrapContentHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Text(
                modifier = Modifier.wrapContentSize(),
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.weight(1.0f))
            StationCount(
                iconRes = R.drawable.wifi_14sp,
                iconDescription = stringResource(R.string.wifi_icon_description),
                count = report.wifiAccessPointCount,
            )
            Spacer(modifier = Modifier.width(2.dp))
            StationCount(
                iconRes = R.drawable.cell_tower_14sp,
                iconDescription = stringResource(R.string.cell_tower_icon_description),
                count = report.cellTowerCount,
            )
            Spacer(modifier = Modifier.width(2.dp))
            StationCount(
                iconRes = R.drawable.bluetooth_14sp,
                iconDescription = stringResource(R.string.bluetooth_icon_description),
                count = report.bluetoothBeaconCount,
            )
        }
        if (canShowMap) {
            Link(
                text = address.value,
                onClick = {
                    context.startActivity(intent)
                },
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Text(
                text = address.value,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StationCount(iconRes: Int, iconDescription: String, count: Int) {
    val decimalFormat = remember { DecimalFormat("0") }

    val localDensity = LocalDensity.current
    var textHeightDp by remember { mutableStateOf(0.dp) }

    Row(
        modifier = Modifier.wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = iconDescription,
            modifier = Modifier.requiredSize(textHeightDp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            modifier = Modifier
                .wrapContentWidth()
                .fillMaxHeight()
                .onGloballyPositioned {
                    textHeightDp = with(localDensity) {
                        it.size.height.toDp()
                    }
                },
            text = decimalFormat.format(count),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}