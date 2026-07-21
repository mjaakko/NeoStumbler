package xyz.malkki.neostumbler.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerializationException
import timber.log.Timber
import xyz.malkki.neostumbler.core.report.Report
import xyz.malkki.neostumbler.data.reports.ReportProvider
import xyz.malkki.neostumbler.ichnaea.dto.BluetoothBeaconDto
import xyz.malkki.neostumbler.ichnaea.dto.GeolocateRequestDto
import xyz.malkki.neostumbler.ichnaea.dto.GeolocateResponseDto
import xyz.malkki.neostumbler.ichnaea.dto.WifiAccessPointDto
import xyz.malkki.neostumbler.ichnaeaupload.IchnaeaClientProvider

private val GEOLOCATE_RETRY_DELAY = 20.seconds

class ReportDetailsViewModel(
    reportId: Long,
    reportProvider: ReportProvider,
    private val ichnaeaClientProvider: IchnaeaClientProvider,
) : ViewModel() {
    val report: StateFlow<Report?> =
        reportProvider
            .getReport(reportId)
            .stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeout = 5.seconds),
                initialValue = null,
            )

    val estimatedReportLocation: StateFlow<GeolocateResponseDto?> =
        report
            .filterNotNull()
            .flatMapLatest { report ->
                ichnaeaClientProvider.ichnaeaClient.filterNotNull().map { ichnaeaClient ->
                    try {
                        ichnaeaClient.getLocation(report.toGeolocateRequestDto())
                    } catch (se: SerializationException) {
                        // The server can return an invalid response -> catch SerializationException
                        Timber.w(se, "Failed to parse geolocation response")

                        null
                    }
                }
            }
            .retryWhen { ex, _ ->
                if (ex is IOException) {
                    Timber.w(ex, "Failed to find a location for the report")

                    delay(GEOLOCATE_RETRY_DELAY)
                } else {
                    throw ex
                }

                true
            }
            .stateIn(
                viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeout = 5.seconds),
                initialValue = null,
            )
}

private fun Report.toGeolocateRequestDto(): GeolocateRequestDto {
    return GeolocateRequestDto(
        considerIp = false,
        bluetoothBeacons =
            bluetoothBeacons.map {
                BluetoothBeaconDto(
                    macAddress = it.emitter.macAddress.value,
                    signalStrength = it.emitter.signalStrength.dbm,
                )
            },
        wifiAccessPoints =
            wifiAccessPoints.map {
                WifiAccessPointDto(
                    macAddress = it.emitter.macAddress.value,
                    signalStrength = it.emitter.signalStrength?.dbm,
                )
            },
        cellTowers =
            cellTowers
                .filter {
                    it.emitter.cellId != null &&
                        it.emitter.mobileCountryCode != null &&
                        it.emitter.mobileNetworkCode != null
                }
                .map {
                    GeolocateRequestDto.CellTowerDto(
                        radioType = it.emitter.radioType.name.lowercase(),
                        mobileCountryCode = it.emitter.mobileCountryCode?.toIntOrNull(),
                        mobileNetworkCode = it.emitter.mobileNetworkCode?.toIntOrNull(),
                        locationAreaCode = it.emitter.locationAreaCode,
                        cellId = it.emitter.cellId,
                        signalStrength = it.emitter.signalStrength?.dbm,
                        psc = it.emitter.primaryScramblingCode,
                        timingAdvance = it.emitter.timingAdvance,
                    )
                },
    )
}
