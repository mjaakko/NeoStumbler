package xyz.malkki.neostumbler.ichnaea

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningReduce
import xyz.malkki.neostumbler.db.dao.ReportDao
import xyz.malkki.neostumbler.db.entities.ReportWithData
import xyz.malkki.neostumbler.extensions.roundToMultipleOf
import xyz.malkki.neostumbler.ichnaea.dto.BluetoothBeaconDto
import xyz.malkki.neostumbler.ichnaea.dto.CellTowerDto
import xyz.malkki.neostumbler.ichnaea.dto.ReportDto
import xyz.malkki.neostumbler.ichnaea.dto.WifiAccessPointDto

// Send max 2000 reports in one request to avoid creating too large payloads
private const val MAX_REPORTS_PER_BATCH = 2000

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
        val reportBatchFlow = channelFlow {
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
                .forEach { send(it) }
        }

        reportBatchFlow.sendBatches(reducedMetadata).collect { reportsSent ->
            progressListener?.invoke(reportsSent)
        }
    }

    suspend fun sendNotUploadedReports(
        reducedMetadata: Boolean,
        progressListener: (suspend (Int) -> Unit)? = null,
    ) {
        val reportBatchFlow = flow {
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

                emit(batch)
            }
        }

        reportBatchFlow.sendBatches(reducedMetadata).collect { reportsSent ->
            progressListener?.invoke(reportsSent)
        }
    }

    private fun Flow<List<ReportWithData>>.sendBatches(reduceMetadata: Boolean): Flow<Int> {
        return buffer(1, onBufferOverflow = BufferOverflow.SUSPEND)
            .map { reportBatch ->
                val dtos =
                    reportBatch.map { report ->
                        if (reduceMetadata) {
                            report.toDto().reduceMetadata()
                        } else {
                            report.toDto()
                        }
                    }

                geosubmit.sendReports(dtos)

                val now = Instant.now()

                val updatedReports =
                    reportBatch
                        .filter {
                            // Do not update upload timestamp for reports which were reuploaded
                            !it.report.uploaded
                        }
                        .map { it.report.copy(uploaded = true, uploadTimestamp = now) }
                        .toTypedArray()

                reportDao.update(*updatedReports)

                reportBatch.size
            }
            .runningReduce { a, b -> a + b }
    }

    private fun ReportWithData.toDto(): ReportDto {
        return ReportDto(
            timestamp = report.timestamp.toEpochMilli(),
            position = ReportDto.PositionDto.fromDbEntity(positionEntity),
            wifiAccessPoints =
                wifiAccessPointEntities.map(WifiAccessPointDto::fromDbEntity).takeIf {
                    it.isNotEmpty()
                },
            cellTowers =
                cellTowerEntities.map(CellTowerDto::fromDbEntity).takeIf { it.isNotEmpty() },
            bluetoothBeacons =
                bluetoothBeaconEntities.map(BluetoothBeaconDto::fromDbEntity).takeIf {
                    it.isNotEmpty()
                },
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
