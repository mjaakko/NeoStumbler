package xyz.malkki.neostumbler.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Build
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.MainActivity
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.db.entities.ReportWithStats
import xyz.malkki.neostumbler.extensions.checkMissingPermissions
import xyz.malkki.neostumbler.extensions.defaultLocale
import xyz.malkki.neostumbler.extensions.getActivity
import xyz.malkki.neostumbler.scanner.ScannerService
import xyz.malkki.neostumbler.scanner.quicksettings.ScannerTileService
import xyz.malkki.neostumbler.ui.composables.AddQSTileDialog
import xyz.malkki.neostumbler.ui.composables.BatteryOptimizationsDialog
import xyz.malkki.neostumbler.ui.composables.MLSWarningDialog
import xyz.malkki.neostumbler.ui.composables.PermissionsDialog
import xyz.malkki.neostumbler.ui.composables.ReportUploadButton
import xyz.malkki.neostumbler.ui.composables.getAddress
import xyz.malkki.neostumbler.ui.composables.rememberServiceConnection
import xyz.malkki.neostumbler.ui.viewmodel.ReportsViewModel
import xyz.malkki.neostumbler.utils.OneTimeActionHelper
import xyz.malkki.neostumbler.utils.geocoder.CachingGeocoder
import xyz.malkki.neostumbler.utils.geocoder.Geocoder
import xyz.malkki.neostumbler.utils.geocoder.PlatformGeocoder
import xyz.malkki.neostumbler.utils.showMapWithMarkerIntent
import java.util.Date
import android.location.Geocoder as AndroidGeocoder

@Composable
fun ReportsScreen() {
    MLSWarningDialog()

    Column {
        Row {
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

    val reportsTotal = reportsViewModel.reportsTotal.observeAsState()
    val reportsNotUploaded = reportsViewModel.reportsNotUploaded.observeAsState()
    val reportsLastUploaded = reportsViewModel.lastUpload.observeAsState()

    val lastUploadedText = reportsLastUploaded.value?.let {
        val millis = it.toEpochMilli()

        DateFormat.getMediumDateFormat(context).format(millis) + " " + DateFormat.getTimeFormat(context).format(millis)
    }

    Column(modifier = Modifier
        .wrapContentHeight()) {
        Text(text = stringResource(R.string.reports_total, reportsTotal.value ?: ""))
        Text(text = stringResource(R.string.reports_not_uploaded, reportsNotUploaded.value ?: ""))
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

    val showBatteryOptimizationsDialog = remember {
        mutableStateOf(false)
    }

    val showPermissionDialog = remember {
        mutableStateOf(false)
    }

    val showBackgroundLocationPermissionDialog = remember {
        mutableStateOf(false)
    }

    val showQuickSettingsDialog = remember {
        mutableStateOf(false)
    }

    val missingPermissions = remember {
        context.checkMissingPermissions(*requiredPermissions)
    }

    fun startScanning() {
        if (ScannerService.serviceRunning) {
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
            Toast.makeText(context, context.getString(R.string.permissions_not_granted), Toast.LENGTH_SHORT).show()
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
        val stringResId = if (serviceConnection.value != null) {
            R.string.stop_scanning
        } else {
            R.string.start_scanning
        }

        Text(text = stringResource(stringResId))
    }
}

@Composable
private fun Reports(reportsViewModel: ReportsViewModel = viewModel()) {
    val reports = reportsViewModel.reports.observeAsState(initial = emptyList())

    val context = LocalContext.current
    val geocoder = remember {
        CachingGeocoder(PlatformGeocoder(AndroidGeocoder(context, context.defaultLocale), 1))
    }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = stringResource(R.string.reports), style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
        LazyColumn {
            itemsIndexed(
                reports.value,
                key =  { _: Int, report: ReportWithStats -> report.reportId },
                itemContent = { _, report ->
                    Report(report = report, geocoder = geocoder)
                }
            )
        }
    }
}

@Composable
private fun Report(report: ReportWithStats, geocoder: Geocoder) {
    val context = LocalContext.current

    val address = getAddress(report.latitude, report.longitude, geocoder = geocoder)

    val date = Date.from(report.timestamp)
    val dateStr = "${DateFormat.getMediumDateFormat(context).format(date)} ${DateFormat.getTimeFormat(context).format(date)}"

    val intent = showMapWithMarkerIntent(report.latitude, report.longitude)
    val canShowMap = intent.resolveActivity(context.packageManager) != null

    Column(modifier = Modifier
        .padding(vertical = 4.dp)
        .wrapContentHeight()) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()) {
            Text(modifier = Modifier.wrapContentSize(), text = dateStr, style = TextStyle(fontSize = 14.sp))
            Spacer(modifier = Modifier.weight(1.0f))
            StationCount(iconRes = R.drawable.wifi_14sp, iconDescription = stringResource(R.string.wifi_icon_description), count = report.wifiAccessPointCount)
            Spacer(modifier = Modifier.width(2.dp))
            StationCount(iconRes = R.drawable.cell_tower_14sp, iconDescription = stringResource(R.string.cell_tower_icon_description), count = report.cellTowerCount)
            Spacer(modifier = Modifier.width(2.dp))
            StationCount(iconRes = R.drawable.bluetooth_14sp, iconDescription = stringResource(R.string.bluetooth_icon_description), count = report.bluetoothBeaconCount)
        }
        if (canShowMap) {
            ClickableText(text = AnnotatedString(address.value), style = TextStyle(fontSize = 14.sp, color = Color.Blue), onClick = {
                context.startActivity(intent)
            })
        } else {
            Text(text = address.value, style = TextStyle(fontSize = 14.sp))
        }
    }
}

@Composable
private fun StationCount(iconRes: Int, iconDescription: String, count: Int) {
    Row(modifier = Modifier.wrapContentSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Icon(painter = painterResource(iconRes), contentDescription = iconDescription)
        Spacer(modifier = Modifier.width(2.dp))
        Text(modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(), text = count.toString(), style = TextStyle(fontSize = 14.sp))
    }
}