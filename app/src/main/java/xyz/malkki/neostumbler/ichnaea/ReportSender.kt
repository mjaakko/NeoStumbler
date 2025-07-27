package xyz.malkki.neostumbler.ichnaea

import java.time.Instant
import java.time.temporal.ChronoUnit
import xyz.malkki.neostumbler.core.report.Report
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.reports.ReportSaver
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

class ReportSender(
    private val geosubmit: Geosubmit,
    private val reportProvider: ReportProvider,
    private val reportSaver: ReportSaver,
) {
    suspend fun reuploadReports(
        from: Instant,
        to: Instant,
        reducedMetadata: Boolean,
        progressListener: (suspend (Int) -> Unit)? = null,
    ) {
        val reportBatches =
            reportProvider
                .getReportsForTimerange(fromTimestamp = from, toTimestamp = to)
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
                    reportProvider.getRandomNotUploadedReports(MAX_REPORTS_PER_BATCH)
                } else {
                    reportProvider.getNotUploadedReports(MAX_REPORTS_PER_BATCH)
                }

            if (batch.isEmpty()) {
                break
            }

            batch.sendBatch(reduceMetadata = reducedMetadata)

            sent += batch.size

            progressListener?.invoke(sent)
        }
    }

    private suspend fun List<Report>.sendBatch(reduceMetadata: Boolean) {
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
                    !it.uploaded
                }
                .map { it.id }

        reportSaver.markAsUploaded(uploadTimestamp = now, *updatedReports.toLongArray())
    }

    private fun Report.toDto(): ReportDto {
        return ReportDto(
            timestamp = timestamp.toEpochMilli(),
            position = position.toDto(),
            wifiAccessPoints = wifiAccessPoints.map { it.toDto() }.takeIf { it.isNotEmpty() },
            cellTowers = cellTowers.map { it.toDto() }.takeIf { it.isNotEmpty() },
            bluetoothBeacons = bluetoothBeacons.map { it.toDto() }.takeIf { it.isNotEmpty() },
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
