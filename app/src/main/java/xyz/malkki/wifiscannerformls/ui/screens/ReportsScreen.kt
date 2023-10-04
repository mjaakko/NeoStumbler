package xyz.malkki.wifiscannerformls.ui.screens

import android.Manifest
import android.location.Geocoder
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.malkki.wifiscannerformls.R
import xyz.malkki.wifiscannerformls.ReportsViewModel
import xyz.malkki.wifiscannerformls.db.entities.ReportWithStats
import xyz.malkki.wifiscannerformls.extensions.checkMissingPermissions
import xyz.malkki.wifiscannerformls.extensions.defaultLocale
import xyz.malkki.wifiscannerformls.scanner.ScannerService
import xyz.malkki.wifiscannerformls.ui.composables.PermissionsDialog
import xyz.malkki.wifiscannerformls.ui.composables.ReportUploadButton
import xyz.malkki.wifiscannerformls.ui.composables.getAddress
import xyz.malkki.wifiscannerformls.ui.composables.rememberServiceConnection
import xyz.malkki.wifiscannerformls.utils.showMapWithMarkerIntent
import java.util.Date

@Composable
fun ReportsScreen() {
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
        Text(text = "Reports total: ${reportsTotal.value ?: ""}")
        Text(text = "Reports not uploaded: ${reportsNotUploaded.value ?: ""}")
        Text(text = "Reports last uploaded: ${lastUploadedText ?: ""}")
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
    }
    .toTypedArray()

@Composable
fun ForegroundScanningButton() {
    val context = LocalContext.current

    val serviceConnection = rememberServiceConnection(getService = ScannerService.ScannerServiceBinder::getService)

    val showPermissionDialog = remember {
        mutableStateOf(false)
    }

    val missingPermissions = remember {
        context.checkMissingPermissions(*requiredPermissions)
    }

    val permissionRationales = mutableMapOf<String, String>().apply {
        put(Manifest.permission.ACCESS_FINE_LOCATION, "Scanning Wi-Fi networks needs access to exact location")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            put(Manifest.permission.POST_NOTIFICATIONS, "Showing status notification needs notification permission")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            put(Manifest.permission.BLUETOOTH_SCAN, "Scanning Bluetooth devices needs access to Bluetooth permission")
        } else {
            put(Manifest.permission.BLUETOOTH, "Scanning Bluetooth devices needs access to Bluetooth permission")
            put(Manifest.permission.BLUETOOTH_ADMIN, "Scanning Bluetooth devices needs access to Bluetooth permission")
        }
    }.toMap()

    val onPermissionsGranted: (Map<String, Boolean>) -> Unit = { permissions ->
        showPermissionDialog.value = false

        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            context.startForegroundService(ScannerService.startIntent(context))
        } else {
            Toast.makeText(context, "Cannot start scanning, because location permission was denied", Toast.LENGTH_SHORT).show()
        }
    }

    if (showPermissionDialog.value) {
        PermissionsDialog(
            missingPermissions = missingPermissions,
            permissionRationales = permissionRationales,
            onPermissionsGranted = onPermissionsGranted
        )
    }

    Button(
        onClick = {
            if (ScannerService.serviceRunning) {
                context.startService(ScannerService.stopIntent(context))
            } else {
                if (Manifest.permission.ACCESS_FINE_LOCATION !in missingPermissions) {
                    context.startForegroundService(ScannerService.startIntent(context))
                } else {
                    showPermissionDialog.value = true
                }
            }
        }
    ) {
        val action = if (serviceConnection.value != null) {
            "Stop"
        } else {
            "Start"
        }

        Text(text = "$action scanning")
    }
}

@Composable
private fun Reports(reportsViewModel: ReportsViewModel = viewModel()) {
    val reports = reportsViewModel.reports.observeAsState(initial = emptyList())

    val context = LocalContext.current
    val geocoder = remember {
        Geocoder(context, context.defaultLocale)
    }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = "Reports:", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
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
            StationCount(iconRes = R.drawable.wifi_14sp, iconDescription = "Wi-Fi icon", count = report.wifiAccessPointCount)
            Spacer(modifier = Modifier.width(2.dp))
            StationCount(iconRes = R.drawable.cell_tower_14sp, iconDescription = "Cell tower icon", count = report.cellTowerCount)
            Spacer(modifier = Modifier.width(2.dp))
            StationCount(iconRes = R.drawable.bluetooth_14sp, iconDescription = "Bluetooth icon", count = report.bluetoothBeaconCount)
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