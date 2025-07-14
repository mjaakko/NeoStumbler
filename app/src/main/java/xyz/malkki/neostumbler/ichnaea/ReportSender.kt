package xyz.malkki.neostumbler.ichnaea

import java.time.Instant
import java.time.temporal.ChronoUnit
import xyz.malkki.neostumbler.db.dao.ReportDao
import xyz.malkki.neostumbler.db.entities.ReportWithData
import xyz.malkki.neostumbler.extensions.roundToMultipleOf
import xyz.malkki.neostumbler.ichnaea.dto.ReportDto
import xyz.malkki.neostumbler.ichnaea.mapper.toDto

/**
 * Limit the number of reports per batch to 999
 *
 * This is used for:
 * 1. avoiding too large payloads
 * 2. not exceeding SQLite variable limit on older Android versions (see
 *    https://www.sqlite.org/limits.html)
 */
private const val MAX_REPORTS_PER_BATCH = 999

// Send speed with 2 m/s accuracy
private const val REDUCED_METADATA_SPEED_ACCURACY = 2.0

// Send heading with 30 degree accuracy
private const val REDUCED_METADATA_HEADING_ACCURACY = 30.0

class ReportSender(private val geosubmit: Geosubmit, private val reportDao: ReportDao) {
    suspend fun reuploadReports(
        from: Instant,
        to: Instant,
        reducedMetadata: Boolean,
        progressListener: (suspend (Int) -> Unit)? = null,
    ) {
        val reportBatches =
            reportDao
                .getAllReportsForTimerange(from, to)
                .let {
                    if (reducedMetadata) {
                        it.shuffled()
                    } else {
                        it
                    }
                }
                .chunked(MAX_REPORTS_PER_BATCH)

        var sent = 0

        reportBatches.forEach {
            it.sendBatch(reduceMetadata = reducedMetadata)

            sent += it.size

            progressListener?.invoke(sent)
        }
    }

    suspend fun sendNotUploadedReports(
        reducedMetadata: Boolean,
        progressListener: (suspend (Int) -> Unit)? = null,
    ) {
        var sent = 0

        while (true) {
            val batch =
                if (reducedMetadata) {
                    reportDao.getRandomNotUploadedReports(MAX_REPORTS_PER_BATCH)
                } else {
                    reportDao.getNotUploadedReports(MAX_REPORTS_PER_BATCH)
                }

            if (batch.isEmpty()) {
                break
            }

            batch.sendBatch(reduceMetadata = reducedMetadata)

            sent += batch.size

            progressListener?.invoke(sent)
        }
    }

    private suspend fun List<ReportWithData>.sendBatch(reduceMetadata: Boolean) {
        val dtos = map { report ->
            if (reduceMetadata) {
                report.toDto().reduceMetadata()
            } else {
                report.toDto()
            }
        }

        geosubmit.sendReports(dtos)

        val now = Instant.now()

        val updatedReports =
            filter {
                    // Do not update upload timestamp for reports which were reuploaded
                    !it.report.uploaded
                }
                .map { it.report.copy(uploaded = true, uploadTimestamp = now) }
                .toTypedArray()

        reportDao.update(*updatedReports)
    }

    private fun ReportWithData.toDto(): ReportDto {
        return ReportDto(
            timestamp = report.timestamp.toEpochMilli(),
            position = positionEntity.toDto(),
            wifiAccessPoints =
                wifiAccessPointEntities.map { it.toDto() }.takeIf { it.isNotEmpty() },
            cellTowers = cellTowerEntities.map { it.toDto() }.takeIf { it.isNotEmpty() },
            bluetoothBeacons = bluetoothBeaconEntities.map { it.toDto() }.takeIf { it.isNotEmpty() },
        )
    }

    private fun ReportDto.reduceMetadata(): ReportDto {
        return copy(
            timestamp = Instant.ofEpochMilli(timestamp).truncatedTo(ChronoUnit.DAYS).toEpochMilli(),
            position =
                position.copy(
                    speed = position.speed?.roundToMultipleOf(REDUCED_METADATA_SPEED_ACCURACY),
                    heading =
                        position.heading?.roundToMultipleOf(REDUCED_METADATA_HEADING_ACCURACY),
                    // Air pressure data is not useful without an accurate timestamp -> set to null
                    pressure = null,
                ),
        )
    }
}
