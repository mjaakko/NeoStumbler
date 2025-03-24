package xyz.malkki.neostumbler.ui.composables.reports

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DecimalFormat
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.scanner.ScannerService
import xyz.malkki.neostumbler.ui.composables.shared.Gauge

@Composable
fun GpsStatus() {
    val decimalFormat = DecimalFormat("#")

    val gpsStats by ScannerService.gpsStats.collectAsStateWithLifecycle()

    val inUsePercentage =
        gpsStats?.let { it.satellitesUsedInFix / it.satellitesTotal.toFloat() } ?: 0f

    val description =
        stringResource(
            R.string.satellites_in_use,
            gpsStats?.satellitesUsedInFix ?: 0,
            gpsStats?.satellitesTotal ?: 0,
        )

    Column(
        modifier =
            Modifier.size(48.dp).semantics(mergeDescendants = true) {
                contentDescription = description
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(32.dp)) {
            Gauge(
                modifier = Modifier.fillMaxSize(),
                percentage = inUsePercentage,
                backgroundColor = MaterialTheme.colorScheme.surfaceDim,
            )

            Icon(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                painter = rememberVectorPainter(Icons.Default.SatelliteAlt),
                contentDescription = null,
            )
        }

        val text = buildString {
            val satellitesInUse = gpsStats?.satellitesUsedInFix?.let { decimalFormat.format(it) }

            append(satellitesInUse ?: "-")

            append(" / ")

            val satellitesTotal = gpsStats?.satellitesTotal?.let { decimalFormat.format(it) }

            append(satellitesTotal ?: "-")
        }

        Text(text = text, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
