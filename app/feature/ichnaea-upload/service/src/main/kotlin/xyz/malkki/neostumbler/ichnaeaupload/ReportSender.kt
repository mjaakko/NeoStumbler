package xyz.malkki.neostumbler.ichnaeaupload

import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import xyz.malkki.neostumbler.core.MacAddress
import xyz.malkki.neostumbler.core.Position
import xyz.malkki.neostumbler.core.emitter.BluetoothBeacon
import xyz.malkki.neostumbler.core.emitter.CellTower
import xyz.malkki.neostumbler.core.emitter.WifiAccessPoint
import xyz.malkki.neostumbler.core.report.Report
import xyz.malkki.neostumbler.core.report.ReportEmitter
import xyz.malkki.neostumbler.core.report.ReportPosition
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.data.reports.ReportSaver
import xyz.malkki.neostumbler.data.settings.Settings
import xyz.malkki.neostumbler.data.settings.getBooleanFlow
import xyz.malkki.neostumbler.ichnaea.Geosubmit
import xyz.malkki.neostumbler.ichnaea.dto.BluetoothBeaconDto
import xyz.malkki.neostumbler.ichnaea.dto.CellTowerDto
import xyz.malkki.neostumbler.ichnaea.dto.ReportDto
import xyz.malkki.neostumbler.ichnaea.dto.ReportDto.PositionDto
import xyz.malkki.neostumbler.ichnaea.dto.WifiAccessPointDto

/**
 * Limit the number of reports per batch to 950
 *
 * This is used for:
 * 1. avoiding too large payloads
 * 2. not exceeding SQLite variable limit on older Android versions (see
 *    https://www.sqlite.org/limits.html)
 *     * Note that this also includes columns to be updated with a variable so the batch size has to
 *       be smaller than the SQLite limit of 999
 */
private const val MAX_REPORTS_PER_BATCH = 950

// Send speed with 2 m/s accuracy
private const val REDUCED_METADATA_SPEED_ACCURACY = 2.0

// Send heading with 30 degree accuracy
private const val REDUCED_METADATA_HEADING_ACCURACY = 30.0

class ReportSender(
    private val geosubmit: Geosubmit,
    private val reportProvider: ReportProvider,
    private val reportSaver: ReportSaver,
    private val settings: Settings,
) {
    suspend fun reuploadReports(
        from: Instant,
        to: Instant,
        progressListener: (suspend (Int) -> Unit)? = null,
    ) {
        val reducedMetadata =
            settings.getBooleanFlow(IchnaeaPreferenceKeys.REDUCED_METADATA, false).first()

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

    suspend fun sendNotUploadedReports(progressListener: (suspend (Int) -> Unit)? = null) {
        val reducedMetadata =
            settings.getBooleanFlow(IchnaeaPreferenceKeys.REDUCED_METADATA, false).first()

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

private fun Double.roundToMultipleOf(multiple: Double) = (this / multiple).roundToInt() * multiple

private fun ReportEmitter<BluetoothBeacon, MacAddress>.toDto(): BluetoothBeaconDto {
    return BluetoothBeaconDto(
        macAddress = emitter.macAddress.value,
        name = null,
        beaconType = emitter.beaconType,
        id1 = emitter.id1,
        id2 = emitter.id2,
        id3 = emitter.id3,
        signalStrength = emitter.signalStrength,
        age = age,
    )
}

private fun ReportEmitter<CellTower, String>.toDto(): CellTowerDto {
    return CellTowerDto(
        radioType = emitter.radioType.name.lowercase(),
        mobileCountryCode = emitter.mobileCountryCode?.toIntOrNull(),
        mobileCountryCodeStr = emitter.mobileCountryCode,
        mobileNetworkCode = emitter.mobileNetworkCode?.toIntOrNull(),
        mobileNetworkCodeStr = emitter.mobileNetworkCode,
        locationAreaCode = emitter.locationAreaCode,
        cellId = emitter.cellId,
        asu = emitter.asu,
        primaryScramblingCode = emitter.primaryScramblingCode,
        serving = emitter.serving,
        signalStrength = emitter.signalStrength,
        timingAdvance = emitter.timingAdvance,
        arfcn = emitter.arfcn,
        age = age,
    )
}

private fun ReportEmitter<WifiAccessPoint, MacAddress>.toDto(): WifiAccessPointDto {
    return WifiAccessPointDto(
        macAddress = emitter.macAddress.value,
        radioType = emitter.radioType?.to802String(),
        ssid = emitter.ssid,
        channel = emitter.channel,
        frequency = emitter.frequency,
        signalStrength = emitter.signalStrength,
        signalToNoiseRatio = null,
        age = age,
    )
}

private fun ReportPosition.toDto(): PositionDto {
    return PositionDto(
        latitude = position.latitude,
        longitude = position.longitude,
        accuracy = position.accuracy?.takeUnless { it.isNaN() },
        age = age,
        altitude = position.altitude?.takeUnless { it.isNaN() },
        altitudeAccuracy = position.altitudeAccuracy?.takeUnless { it.isNaN() },
        heading = position.heading?.takeUnless { it.isNaN() },
        pressure = position.pressure?.takeUnless { it.isNaN() },
        speed = position.speed?.takeUnless { it.isNaN() },
        // Ichnaea Geosubmit officially only supports these sources
        // https://ichnaea.readthedocs.io/en/latest/api/geosubmit2.html#position-fields
        source =
            if (position.source == Position.Source.GPS) {
                "gps"
            } else {
                "fused"
            },
    )
}
