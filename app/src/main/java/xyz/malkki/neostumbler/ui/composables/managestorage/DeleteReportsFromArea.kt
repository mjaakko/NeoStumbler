package xyz.malkki.neostumbler.ui.composables.managestorage

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.room.withTransaction
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.db.ReportDatabase
import xyz.malkki.neostumbler.db.dao.getReportsInsideBoundingBox
import xyz.malkki.neostumbler.domain.LatLng
import xyz.malkki.neostumbler.extensions.getQuantityString
import xyz.malkki.neostumbler.extensions.showToast
import xyz.malkki.neostumbler.ui.composables.shared.AreaPickerDialog

@Composable
fun DeleteReportsFromArea(reportDb: StateFlow<ReportDatabase>) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    val dialogOpen = rememberSaveable { mutableStateOf(false) }

    if (dialogOpen.value) {
        AreaPickerDialog(
            title = stringResource(R.string.delete_reports_from_area),
            positiveButtonText = stringResource(R.string.delete),
            onAreaSelected = { circle ->
                dialogOpen.value = false

                if (circle != null) {
                    val (center, radius) = circle

                    // Because we don't have any geographic functions available in SQLite,
                    // we can't delete reports directly. Instead, first query reports inside a
                    // bounding box and then filter the ones inside the circle
                    val (bottomLeft, topRight) = getBoundingBoxForCircle(center, radius)

                    coroutineScope.launch {
                        val db = reportDb.value

                        val deletedCount =
                            db.withTransaction {
                                val reportDao = db.reportDao()

                                val reportsInsideBoundingBox =
                                    reportDao
                                        .getReportsInsideBoundingBox(
                                            minLatitude = bottomLeft.latitude,
                                            minLongitude = bottomLeft.longitude,
                                            maxLatitude = topRight.latitude,
                                            maxLongitude = topRight.longitude,
                                        )
                                        .first()

                                val reportsToDelete =
                                    reportsInsideBoundingBox
                                        .filter { report ->
                                            LatLng(
                                                    latitude = report.latitude,
                                                    longitude = report.longitude,
                                                )
                                                .distanceTo(center) <= radius
                                        }
                                        .map { it.id }
                                        .toLongArray()

                                reportDao.delete(*reportsToDelete)
                            }

                        context.showToast(
                            context.getQuantityString(
                                R.plurals.toast_deleted_reports,
                                deletedCount,
                                deletedCount,
                            )
                        )
                    }
                }
            },
        )
    }

    Button(onClick = { dialogOpen.value = true }) {
        Text(text = stringResource(R.string.delete_reports_from_area))
    }
}

private fun getBoundingBoxForCircle(center: LatLng, radius: Double): Pair<LatLng, LatLng> {
    val topRight =
        center
            .destination(distance = radius, bearing = 0.0)
            .destination(distance = radius, bearing = 90.0)
    val bottomLeft =
        center
            .destination(distance = radius, bearing = 180.0)
            .destination(distance = radius, bearing = 270.0)

    return bottomLeft to topRight
}
